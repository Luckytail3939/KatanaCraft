package luckytail.katanacraft.combat;

import luckytail.katanacraft.Config;
import luckytail.katanacraft.item.KatanaItem;
import luckytail.katanacraft.network.ModNetwork;
import luckytail.katanacraft.network.S2CActionPacket;
import luckytail.katanacraft.network.S2CCombatStatePacket;
import luckytail.katanacraft.registry.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側の戦闘ロジック本体。
 *  - 入力 → モーション開始（先行入力バッファ付き）
 *  - モーションの進行（ヒットフレームで斬撃判定）
 *  - 弾き / 通常ガード / ガード崩し / 体幹
 *  - ルーン入力と固有技の発動
 *  - クライアントへの状態同期
 */
public final class CombatManager {
    private CombatManager() {}

    private static final Map<UUID, CombatState> STATES = new ConcurrentHashMap<>();
    private static final UUID GUARD_SLOW_ID = UUID.fromString("b3a9e7a4-61c2-4f0f-9f63-0e3d8d2b7c11");
    /** モーション終了の何tick前から次の入力を受け付けて予約するか */
    private static final int BUFFER_TICKS = 6;
    /** 1・2段目の後、次段へ繋げられる猶予 */
    private static final int COMBO_WINDOW = 12;
    /** 抜いたまま終わるモーションの後、納刀するまで (ticks)。次段の受付より少し長く */
    private static final int SHEATHE_DELAY = COMBO_WINDOW + 2;
    /** ガードを離してから納刀するまで (ticks) */
    private static final int SHEATHE_AFTER_GUARD = 8;
    private static final DustParticleOptions EMPOWER_DUST = new DustParticleOptions(new Vector3f(0.75F, 0.5F, 1.0F), 0.9F);

    public static CombatState get(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new CombatState());
    }

    public static void remove(ServerPlayer p) {
        STATES.remove(p.getUUID());
    }

    private static float maxPosture() {
        return Config.MAX_POSTURE.get().floatValue();
    }

    private static boolean isStunned(ServerPlayer p) {
        return p.hasEffect(ModEffects.GUARD_BREAK.get());
    }

    // ------------------------------------------------------------------
    // 入力
    // ------------------------------------------------------------------

    public static void onInput(ServerPlayer p, InputType input) {
        if (p.isSpectator() || !KatanaItem.isHoldingKatana(p) || isStunned(p)) return;
        CombatState s = get(p);
        if (s.action != null) {
            // モーション終盤なら先行入力として予約
            if (s.actionDuration - s.actionTick <= BUFFER_TICKS) s.buffered = input;
            return;
        }
        execute(p, s, input);
    }

    public static void onGuardInput(ServerPlayer p, boolean down) {
        CombatState s = get(p);
        if (down && !s.guardHeld) {
            if (KatanaItem.isHoldingKatana(p) && !isStunned(p)) {
                // 弾き受付開始（連打対策のクールダウン付き）
                if (p.tickCount - s.lastGuardPressTick >= Config.PARRY_REPRESS_COOLDOWN_TICKS.get()) {
                    s.parryTicks = Config.PARRY_WINDOW_TICKS.get();
                }
                s.lastGuardPressTick = p.tickCount;
                // ガードはモーションをキャンセルできる（ルーンはリセット）
                if (s.action != null && s.action != KatanaAction.ART) {
                    s.action = null;
                    s.buffered = null;
                }
            }
        }
        if (!down) s.guardLocked = false;
        if (down) s.sheatheTimer = 0;
        else if (s.guardHeld && s.action == null) s.sheatheTimer = SHEATHE_AFTER_GUARD;
        s.guardHeld = down;
        s.dirty = true;
    }

    private static void execute(ServerPlayer p, CombatState s, InputType input) {
        KatanaItem katana = (KatanaItem) p.getMainHandItem().getItem();

        // 最後のルーンが揃う入力なら固有技（体幹消費なし）
        if (completesArt(s, katana, input)) {
            s.runes.add(input);
            s.runeTimer = Config.RUNE_TIMEOUT_TICKS.get();
            s.artFiredPending = true; // 同期後にクリア（HUDで全ルーンを光らせるため）
            s.artCooldown = Config.ART_COOLDOWN_TICKS.get();
            s.lightStep = 0;
            s.comboWindow = 0;
            // 強化状態ならこの固有技で消費する
            s.artEmpowered = s.empoweredTicks > 0;
            if (s.artEmpowered) s.empoweredTicks = 0;
            startAction(p, s, KatanaAction.ART, katana.getArtDuration());
            return;
        }

        KatanaAction action = actionFor(s, input);
        float cost = costOf(action);
        if (s.posture < cost) {
            // 体幹不足: 攻撃が出ない（ルーンも刻まれない）
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_WEAK, 0.8F, 0.6F);
            broadcastAction(p, KatanaAction.EXHAUSTED);
            s.dirty = true;
            return;
        }

        inscribeRune(s, katana, input);
        spendPosture(s, cost);
        if (input != InputType.LIGHT_TAP) s.lightStep = 0;
        s.comboWindow = 0;
        startAction(p, s, action, action.duration);
    }

    private static KatanaAction actionFor(CombatState s, InputType input) {
        return switch (input) {
            case LIGHT_TAP -> s.lightStep == 0 ? KatanaAction.LIGHT_1
                    : s.lightStep == 1 ? KatanaAction.LIGHT_2 : KatanaAction.LIGHT_3;
            case LIGHT_HOLD -> KatanaAction.FLURRY;
            case HEAVY_TAP -> KatanaAction.HEAVY;
            case HEAVY_HOLD -> KatanaAction.DASH_STORM;
        };
    }

    /** 攻撃ごとの体幹消費（弱い順: 通常斬撃 < 3段目 < 横薙ぎ < 抜刀連撃 < 踏み込み乱舞） */
    public static float costOf(KatanaAction action) {
        return switch (action) {
            case LIGHT_1, LIGHT_2 -> Config.COST_LIGHT.get().floatValue();
            case LIGHT_3 -> Config.COST_LIGHT_FINISHER.get().floatValue();
            case HEAVY -> Config.COST_HEAVY.get().floatValue();
            case FLURRY -> Config.COST_FLURRY.get().floatValue();
            case DASH_STORM -> Config.COST_DASH_STORM.get().floatValue();
            default -> 0F;
        };
    }

    private static void spendPosture(CombatState s, float cost) {
        if (cost <= 0) return;
        s.posture -= cost;
        int delay = Config.POSTURE_REGEN_DELAY_TICKS.get();
        int attackDelay = Config.ATTACK_REGEN_DELAY_TICKS.get();
        // 攻撃後の回復待ちは被ガード時より短い（ただし直前にガードで削られていたらそちらを優先）
        s.ticksSincePostureHit = Math.min(s.ticksSincePostureHit, Math.max(0, delay - attackDelay));
        s.dirty = true;
    }

    private static boolean completesArt(CombatState s, KatanaItem katana, InputType input) {
        List<InputType> seq = katana.getArtSequence();
        if (seq.isEmpty() || s.artCooldown > 0) return false;
        return s.runes.size() == seq.size() - 1 && seq.get(seq.size() - 1) == input;
    }

    /**
     * ルーンを1つ刻む（列の完成は completesArt 側で扱う）。
     * 列と違う入力をしたらリセット（その入力が列の先頭なら1つ目として刻み直す）。
     */
    private static void inscribeRune(CombatState s, KatanaItem katana, InputType input) {
        List<InputType> seq = katana.getArtSequence();
        if (seq.isEmpty() || s.artCooldown > 0) return;

        if (s.runes.size() < seq.size() - 1 && seq.get(s.runes.size()) == input) {
            s.runes.add(input);
        } else {
            s.runes.clear();
            if (seq.size() > 1 && seq.get(0) == input) s.runes.add(input);
        }
        s.runeTimer = Config.RUNE_TIMEOUT_TICKS.get();
        s.dirty = true;
    }

    private static void startAction(ServerPlayer p, CombatState s, KatanaAction action, int duration) {
        s.action = action;
        s.actionTick = 0;
        s.sheatheTimer = 0;
        s.actionDuration = Math.max(1, duration);
        s.buffered = null;
        s.dirty = true;
        broadcastAction(p, action);
    }

    private static void broadcastAction(ServerPlayer p, KatanaAction action) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                new S2CActionPacket(p.getId(), action.ordinal()));
    }

    // ------------------------------------------------------------------
    // 毎tick
    // ------------------------------------------------------------------

    public static void tick(ServerPlayer p) {
        CombatState s = get(p);
        boolean holding = KatanaItem.isHoldingKatana(p);
        float max = maxPosture();
        if (s.posture < 0) s.posture = max;

        // 持ち替えたらモーションとルーンを破棄
        if (!holding) {
            if (s.action != null || !s.runes.isEmpty()) {
                s.action = null;
                s.buffered = null;
                s.runes.clear();
                s.dirty = true;
            }
            s.lightStep = 0;
        }

        // モーション進行
        if (s.action != null) {
            KatanaAction a = s.action;
            if (a == KatanaAction.ART) {
                ((KatanaItem) p.getMainHandItem().getItem()).artTick(p, s.actionTick, s.artEmpowered);
            } else {
                KatanaMoves.tick(p, a, s.actionTick);
            }
            s.actionTick++;
            if (s.action == a && s.actionTick >= s.actionDuration) {
                finishAction(p, s, a);
            }
        } else if (s.comboWindow > 0 && --s.comboWindow == 0) {
            s.lightStep = 0;
        }

        // 突進連撃のタメ〜突進中は低い姿勢（視点も下がる）
        boolean crouch = holding && s.action == KatanaAction.DASH_STORM && s.actionTick < KatanaMoves.DASH_CROUCH_TICKS;
        if (crouch != s.forcedCrouch) {
            p.setForcedPose(crouch ? Pose.CROUCHING : null);
            s.forcedCrouch = crouch;
        }

        if (s.parryTicks > 0) s.parryTicks--;

        // 納刀
        if (s.sheatheTimer > 0 && --s.sheatheTimer == 0 && s.action == null && !s.guardHeld && holding) {
            broadcastAction(p, KatanaAction.SHEATHE);
            AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F);
        }

        // 弾き回数の時間切れ / 強化状態の残り時間
        if (s.parryStacks > 0 && p.tickCount - s.lastParryTick > Config.PARRY_STACK_RESET_TICKS.get()) {
            s.parryStacks = 0;
            s.dirty = true;
        }
        if (s.empoweredTicks > 0) {
            s.empoweredTicks--;
            if (s.empoweredTicks == 0) s.dirty = true;
            if (holding && p.tickCount % 3 == 0) {
                p.serverLevel().sendParticles(EMPOWER_DUST, p.getX(), p.getY(0.5), p.getZ(), 2, 0.35, 0.5, 0.35, 0.0);
            }
        }

        // ルーンの時間切れ
        if (!s.runes.isEmpty() && !s.artFiredPending && --s.runeTimer <= 0) {
            s.runes.clear();
            s.dirty = true;
        }
        if (s.artCooldown > 0) {
            s.artCooldown--;
            if (s.artCooldown == 0) s.dirty = true;
        }

        // 体幹の自然回復
        s.ticksSincePostureHit++;
        if (s.posture < max && s.ticksSincePostureHit >= Config.POSTURE_REGEN_DELAY_TICKS.get()) {
            float regen = Config.POSTURE_REGEN_PER_TICK.get().floatValue();
            if (s.isGuarding()) regen /= 3F;
            s.posture = Math.min(max, s.posture + regen);
            if (p.tickCount % 2 == 0 || s.posture >= max) s.dirty = true;
        }

        // ガード中の移動低下
        updateGuardSlow(p, holding && s.isGuarding() && !isStunned(p));

        if (s.dirty) sync(p, s);
    }

    private static void finishAction(ServerPlayer p, CombatState s, KatanaAction a) {
        s.action = null;
        switch (a) {
            case LIGHT_1 -> { s.lightStep = 1; s.comboWindow = COMBO_WINDOW; }
            case LIGHT_2 -> { s.lightStep = 2; s.comboWindow = COMBO_WINDOW; }
            default -> { s.lightStep = 0; s.comboWindow = 0; }
        }
        // 刀を抜いたまま終わるモーションは、次の入力がなければ少し後に納刀する
        if (a == KatanaAction.LIGHT_1 || a == KatanaAction.LIGHT_2 || a == KatanaAction.HEAVY) {
            s.sheatheTimer = SHEATHE_DELAY;
        }
        s.dirty = true;
        if (s.buffered != null) {
            InputType b = s.buffered;
            s.buffered = null;
            if (KatanaItem.isHoldingKatana(p) && !isStunned(p)) execute(p, s, b);
        }
    }

    private static void updateGuardSlow(ServerPlayer p, boolean slow) {
        AttributeInstance inst = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) return;
        boolean has = inst.getModifier(GUARD_SLOW_ID) != null;
        if (slow && !has) {
            inst.addTransientModifier(new AttributeModifier(GUARD_SLOW_ID, "katanacraft_guard",
                    -Config.GUARD_MOVE_SLOW.get(), AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (!slow && has) {
            inst.removeModifier(GUARD_SLOW_ID);
        }
    }

    public static void sync(ServerPlayer p, CombatState s) {
        KatanaItem katana = p.getMainHandItem().getItem() instanceof KatanaItem k ? k : null;
        byte[] runes = new byte[s.runes.size()];
        for (int i = 0; i < runes.length; i++) runes[i] = (byte) s.runes.get(i).ordinal();
        int seqLen = katana != null ? katana.getArtSequence().size() : 0;

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new S2CCombatStatePacket(
                Math.max(0, s.posture), maxPosture(), s.isGuarding(), runes, seqLen,
                s.runeTimer, s.artCooldown, Config.ART_COOLDOWN_TICKS.get(), s.artFiredPending,
                s.parryStacks, Config.PARRIES_TO_EMPOWER.get(), s.empoweredTicks, Config.EMPOWER_DURATION_TICKS.get()));

        if (s.artFiredPending) {
            s.artFiredPending = false;
            s.runes.clear();
        }
        s.dirty = false;
    }

    // ------------------------------------------------------------------
    // 防御
    // ------------------------------------------------------------------

    /** 正面からの、盾で防げる種類の攻撃か */
    private static boolean isGuardable(ServerPlayer p, DamageSource src) {
        if (src.is(DamageTypeTags.BYPASSES_SHIELD)) return false;
        Vec3 pos = src.getSourcePosition();
        if (pos == null) return false;
        Vec3 look = AttackHelper.flatLook(p);
        Vec3 to = new Vec3(pos.x - p.getX(), 0, pos.z - p.getZ());
        if (to.lengthSqr() < 1.0E-4) return true;
        return to.normalize().dot(look) > 0.0; // 前方180度
    }

    /**
     * LivingAttackEvent から呼ぶ。弾きが成立したら true（攻撃は無効化される）。
     */
    public static boolean tryParry(ServerPlayer p, DamageSource src) {
        if (!KatanaItem.isHoldingKatana(p) || isStunned(p)) return false;
        CombatState s = get(p);
        if (s.parryTicks <= 0 || !isGuardable(p, src)) return false;

        // --- 弾き成功 ---
        s.guardCount = 0;
        addParryStack(p, s);
        s.posture = Math.min(maxPosture(), s.posture + Config.PARRY_POSTURE_RECOVER.get().floatValue());
        s.dirty = true;

        Vec3 look = AttackHelper.flatLook(p);
        p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,
                p.getX() + look.x * 0.9, p.getEyeY() - 0.3, p.getZ() + look.z * 0.9, 16, 0.15, 0.15, 0.15, 0.4);
        p.serverLevel().sendParticles(ParticleTypes.FLASH,
                p.getX() + look.x * 0.9, p.getEyeY() - 0.3, p.getZ() + look.z * 0.9, 1, 0, 0, 0, 0);
        AttackHelper.sound(p, SoundEvents.ANVIL_LAND, 0.45F, 1.9F);
        AttackHelper.sound(p, SoundEvents.BELL_BLOCK, 0.6F, 1.6F);

        Entity attacker = src.getEntity();
        if (attacker instanceof LivingEntity le && src.getDirectEntity() == attacker) {
            // 近接攻撃をいなす: 攻撃者をのけぞらせて一瞬怯ませる
            le.knockback(0.6, p.getX() - le.getX(), p.getZ() - le.getZ());
            int stagger = Config.PARRY_STAGGER_TICKS.get();
            if (stagger > 0) le.addEffect(new MobEffectInstance(ModEffects.GUARD_BREAK.get(), stagger, 0, false, false, true));
        }
        broadcastAction(p, KatanaAction.PARRY);
        return true;
    }

    /** 弾き回数を加算し、規定回数で刀を強化状態にする */
    private static void addParryStack(ServerPlayer p, CombatState s) {
        if (p.tickCount - s.lastParryTick > Config.PARRY_STACK_RESET_TICKS.get()) s.parryStacks = 0;
        s.lastParryTick = p.tickCount;
        int duration = Config.EMPOWER_DURATION_TICKS.get();
        if (s.empoweredTicks > 0) {
            s.empoweredTicks = duration; // 強化中に弾けば時間を延長
            return;
        }
        s.parryStacks++;
        if (s.parryStacks >= Config.PARRIES_TO_EMPOWER.get()) {
            s.parryStacks = 0;
            s.empoweredTicks = duration;
            AttackHelper.sound(p, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F);
            AttackHelper.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.8F);
            p.serverLevel().sendParticles(EMPOWER_DUST, p.getX(), p.getY(0.5), p.getZ(), 40, 0.6, 0.8, 0.6, 0.0);
            p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY(0.6), p.getZ(), 20, 0.5, 0.6, 0.5, 0.2);
            broadcastAction(p, KatanaAction.EMPOWERED);
        }
    }

    /**
     * LivingHurtEvent から呼ぶ。通常ガードが成立したら軽減後のダメージを返す。
     */
    public static float onGuardHit(ServerPlayer p, DamageSource src, float amount) {
        if (!KatanaItem.isHoldingKatana(p) || isStunned(p)) return amount;
        CombatState s = get(p);
        if (!s.isGuarding() || !isGuardable(p, src)) return amount;

        if (p.tickCount - s.lastGuardHitTick > Config.GUARD_CHAIN_RESET_TICKS.get()) s.guardCount = 0;
        s.guardCount++;
        s.lastGuardHitTick = p.tickCount;

        float cost = Config.GUARD_POSTURE_BASE.get().floatValue() + amount * Config.GUARD_POSTURE_PER_DAMAGE.get().floatValue();
        s.posture -= cost;
        s.ticksSincePostureHit = 0;
        s.dirty = true;

        AttackHelper.sound(p, SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + p.getRandom().nextFloat() * 0.3F);
        Vec3 look = AttackHelper.flatLook(p);
        p.serverLevel().sendParticles(ParticleTypes.CRIT,
                p.getX() + look.x * 0.8, p.getEyeY() - 0.4, p.getZ() + look.z * 0.8, 6, 0.1, 0.1, 0.1, 0.2);

        if (s.guardCount >= Config.GUARD_BREAK_COUNT.get() || s.posture <= 0) {
            breakGuard(p, s);
        } else {
            broadcastAction(p, KatanaAction.GUARD_IMPACT);
        }
        return amount * Config.GUARD_CHIP_RATIO.get().floatValue();
    }

    private static void breakGuard(ServerPlayer p, CombatState s) {
        int t = Config.GUARD_BREAK_TICKS.get();
        p.addEffect(new MobEffectInstance(ModEffects.GUARD_BREAK.get(), t, 0, false, true, true));
        p.addEffect(new MobEffectInstance(ModEffects.VULNERABLE.get(), t, 0, false, true, true));
        s.guardLocked = true;   // キーを離して押し直すまでガード不可
        s.guardCount = 0;
        s.parryStacks = 0;      // 崩されたら弾きの積み上げは失う（強化状態そのものは残る）
        s.posture = maxPosture() * 0.5F;
        s.ticksSincePostureHit = 0;
        s.action = null;
        s.buffered = null;
        s.parryTicks = 0;
        s.dirty = true;

        AttackHelper.sound(p, SoundEvents.SHIELD_BREAK, 1.0F, 0.9F);
        AttackHelper.sound(p, SoundEvents.ITEM_BREAK, 0.6F, 0.6F);
        AttackHelper.particles(p, ParticleTypes.ELECTRIC_SPARK, 20, 0.4, 0.3);
        broadcastAction(p, KatanaAction.GUARD_BROKEN);
    }
}
