package luckytail.katanacraft.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * 全刀共通の通常モーション。各モーションの tick ごとに何が起きるかを書く。
 * （タイミングは KatanaAction の duration と合わせること）
 *
 * 数値の目安: range=届く距離, arc=扇の角度, mult=攻撃力倍率, kb=ノックバック
 */
public final class KatanaMoves {
    private KatanaMoves() {}

    public static void tick(ServerPlayer p, KatanaAction action, int t) {
        switch (action) {
            case LIGHT_1 -> light1(p, t);
            case LIGHT_2 -> light2(p, t);
            case LIGHT_3 -> light3(p, t);
            case FLURRY -> flurry(p, t);
            case HEAVY -> heavy(p, t);
            case DASH_STORM -> dashStorm(p, t);
            default -> {
            }
        }
    }

    /** 1段目: 鞘から片手で抜刀 → 右上から左下へ袈裟斬り */
    private static void light1(ServerPlayer p, int t) {
        if (t == 0) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.6F, 2.0F); // 鯉口を切る音
        if (t == 3) {
            AttackHelper.slash(p, 3.6, 120, 1.0F, 0.25F);
            AttackHelper.sweepFx(p, 1.4, 1);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.3F);
        }
    }

    /** 2段目: 左下から右上へ斬り上げ */
    private static void light2(ServerPlayer p, int t) {
        if (t == 3) {
            AttackHelper.slash(p, 3.6, 120, 1.0F, 0.25F);
            AttackHelper.sweepFx(p, 1.4, 1);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.5F);
        }
    }

    /** 3段目: 両手で掴み上段から振り下ろし → 納刀 */
    private static void light3(ServerPlayer p, int t) {
        if (t == 5) {
            AttackHelper.slash(p, 3.8, 90, 1.6F, 0.5F);
            AttackHelper.sweepFx(p, 1.5, 2);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.1F);
        }
        if (t == 13) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F); // 納刀
    }

    /** 左長押し: 抜刀からの連撃 → 締めの一太刀 → 納刀 */
    private static void flurry(ServerPlayer p, int t) {
        if (t == 0) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.6F, 2.0F);
        if (t >= 4 && t <= 20 && t % 4 == 0) {
            AttackHelper.lunge(p, 0.18, p.getDeltaMovement().y);
            AttackHelper.slash(p, 3.5, 100, 0.5F, 0.05F);
            AttackHelper.sweepFx(p, 1.3, 1);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.4F + (t / 4) * 0.08F);
        }
        if (t == 25) {
            AttackHelper.slash(p, 3.8, 120, 1.2F, 0.6F);
            AttackHelper.sweepFx(p, 1.5, 3);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
        }
        if (t == 31) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F);
    }

    /** 右クリック: 刀を引いてタメ → 横一文字の斬り払い（広い扇・強ノックバック） */
    private static void heavy(ServerPlayer p, int t) {
        if (t == 0) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 0.6F); // 柄を握り直す
        if (t < 14 && t % 3 == 1) {
            // タメ: 刀身の周りに光が集まる
            Vec3 look = AttackHelper.flatLook(p);
            Vec3 side = new Vec3(-look.z, 0, look.x); // 右手側
            p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                    p.getX() + side.x * 0.6 - look.x * 0.3, p.getY(0.6), p.getZ() + side.z * 0.6 - look.z * 0.3,
                    4, 0.25, 0.25, 0.25, 0.05);
        }
        if (t == 10) AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8F, 0.6F);
        if (t == 14) {
            AttackHelper.slash(p, 5.0, 250, 2.0F, 1.0F);
            AttackHelper.sweepFx(p, 2.0, 5);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.75F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 0.8F);
        }
    }

    /** 右長押し: 前方へ踏み込み → 周囲を連続で斬り刻む */
    private static void dashStorm(ServerPlayer p, int t) {
        if (t == 0) {
            AttackHelper.lunge(p, 1.7, 0.1);
            AttackHelper.particles(p, ParticleTypes.CLOUD, 8, 0.3, 0.05);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 0.5F);
        }
        if (t >= 8 && t <= 20 && t % 4 == 0) {
            AttackHelper.slash(p, 4.5, 360, 0.6F, 0.1F);
            AttackHelper.ringFx(p, 2.2, 5);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.0F + (t / 4) * 0.1F);
        }
        if (t == 25) {
            AttackHelper.slash(p, 4.8, 360, 1.4F, 1.0F);
            AttackHelper.ringFx(p, 3.0, 8);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.7F);
        }
        if (t == 31) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F);
    }
}
