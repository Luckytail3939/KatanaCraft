package luckytail.katanacraft.combat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 斬撃判定・演出の共通処理 */
public final class AttackHelper {
    private AttackHelper() {}

    /** 水平方向の視線ベクトル（正規化済み） */
    public static Vec3 flatLook(ServerPlayer p) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /**
     * 扇形の斬撃判定。
     * @param range    届く距離（ブロック）
     * @param arcDeg   扇の角度（360で全周）
     * @param mult     攻撃力倍率（プレイヤーの攻撃力属性に掛ける）
     * @param kb       ノックバック強度
     * @return 命中数
     */
    public static int slash(ServerPlayer p, double range, double arcDeg, float mult, float kb) {
        ServerLevel level = p.serverLevel();
        Vec3 look = flatLook(p);
        ItemStack stack = p.getMainHandItem();
        float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double cosHalf = Math.cos(Math.toRadians(arcDeg / 2.0));
        int fire = EnchantmentHelper.getFireAspect(p);
        AABB box = p.getBoundingBox().inflate(range, 1.0, range);

        int hits = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                t -> t != p && t.isAlive() && !p.isAlliedTo(t) && !t.isSpectator()
                        && !(t instanceof TamableAnimal ta && ta.isOwnedBy(p)))) {
            Vec3 to = e.position().subtract(p.position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            double dist = flat.length();
            if (dist - e.getBbWidth() / 2.0 > range) continue;
            if (arcDeg < 360 && dist > 0.6 && flat.normalize().dot(look) < cosHalf) continue;

            float dmg = base * mult + EnchantmentHelper.getDamageBonus(stack, e.getMobType()) * Math.min(1F, mult);
            e.invulnerableTime = 0; // 連撃を通すため無敵時間を解除
            if (e.hurt(p.damageSources().playerAttack(p), dmg)) {
                hits++;
                if (kb > 0) {
                    e.knockback(kb, Mth.sin(p.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(p.getYRot() * Mth.DEG_TO_RAD));
                }
                if (fire > 0) e.setSecondsOnFire(fire * 4);
                EnchantmentHelper.doPostDamageEffects(p, e);
                level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY(0.6), e.getZ(), 8, 0.2, 0.2, 0.2, 0.3);
            }
        }
        if (hits > 0 && !p.getAbilities().instabuild) {
            stack.hurtAndBreak(1, p, pl -> pl.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        p.swing(InteractionHand.MAIN_HAND, true);
        return hits;
    }

    /** 正面に剣閃パーティクル */
    public static void sweepFx(ServerPlayer p, double forward, int count) {
        Vec3 look = flatLook(p);
        p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK,
                p.getX() + look.x * forward, p.getY(0.55), p.getZ() + look.z * forward,
                count, 0.3, 0.1, 0.3, 0.0);
    }

    /** 周囲に剣閃パーティクル（全周攻撃用） */
    public static void ringFx(ServerPlayer p, double radius, int points) {
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    p.getX() + Math.cos(a) * radius, p.getY(0.5), p.getZ() + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    public static void particles(ServerPlayer p, ParticleOptions type, int count, double spread, double speed) {
        p.serverLevel().sendParticles(type, p.getX(), p.getY(0.6), p.getZ(), count, spread, spread * 0.6, spread, speed);
    }

    public static void sound(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** 視線方向（水平）へ移動させる。負の値で後退。サーバーから速度を同期する */
    public static void lunge(ServerPlayer p, double horizontal, double vertical) {
        Vec3 look = flatLook(p);
        p.setDeltaMovement(look.x * horizontal, vertical, look.z * horizontal);
        p.hurtMarked = true; // プレイヤーへ速度パケットを送らせる
    }
}
