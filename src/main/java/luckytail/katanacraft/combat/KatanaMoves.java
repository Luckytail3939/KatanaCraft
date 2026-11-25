package luckytail.katanacraft.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * 全刀共通の通常モーション（サーバー側）。各モーションの tick ごとに何が起きるかを書く。
 *
 * ここの「当たりの tick」は、player_animation の振り抜く瞬間と、
 * クライアントの斬撃エフェクト（ClientSlashFx）の出る瞬間に合わせてある。
 * 変更するときは3か所をそろえること。
 *
 * 数値の目安: range=届く距離, arc=扇の角度, mult=攻撃力倍率, kb=ノックバック
 */
public final class KatanaMoves {
    private KatanaMoves() {}

    /** 突進連撃で低く構えている（視点が下がる）時間 */
    public static final int DASH_CROUCH_TICKS = 14;

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

    /** 1段目: 居合で抜刀 → 片手で右上から左下へ袈裟斬り（当たり 6） */
    private static void light1(ServerPlayer p, int t) {
        if (t == 0) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.6F, 2.0F); // 鯉口を切る
        if (t == 2) AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.5F, 1.6F);
        if (t == 6) {
            AttackHelper.slash(p, 3.6, 130, 1.0F, 0.25F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.3F);
        }
    }

    /** 2段目: 片手で左下から右上へ斬り上げ（当たり 3） */
    private static void light2(ServerPlayer p, int t) {
        if (t == 3) {
            AttackHelper.slash(p, 3.6, 130, 1.0F, 0.25F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.5F);
        }
    }

    /** 3段目: 両手に持ち替えて真っ向から唐竹割り（当たり 7）→ 納刀（15） */
    private static void light3(ServerPlayer p, int t) {
        if (t == 7) {
            AttackHelper.slash(p, 3.8, 90, 1.6F, 0.5F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.1F);
        }
        if (t == 15) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F); // 納刀
    }

    /** 左長押し: 縦に抜刀し、燕返しのように三方向から同時に斬る（当たり 5,6,7）→ 納刀（15） */
    private static void flurry(ServerPlayer p, int t) {
        if (t == 0) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.6F, 2.0F);
        if (t >= 5 && t <= 7) {
            AttackHelper.slash(p, 3.8, 140, 0.85F, t == 7 ? 0.5F : 0.05F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.3F + (t - 5) * 0.15F);
        }
        if (t == 15) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F);
    }

    /** 右クリック: 左腰の柄に手を添えて溜め → 片手で横一文字の抜き打ち（当たり 14） */
    private static void heavy(ServerPlayer p, int t) {
        if (t == 0) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 0.6F); // 柄を握る
        if (t > 3 && t < 13 && t % 3 == 1) chargeFx(p);
        if (t == 12) AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8F, 0.6F);
        if (t == 14) {
            AttackHelper.slash(p, 5.0, 250, 2.0F, 1.0F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.75F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 0.8F);
        }
    }

    /**
     * 右長押し: 低く構えて溜め（視点が下がる）→ 高速で突進 →
     * 燕返しの三連撃（12,13,14）+ 横一閃（17）+ 返しの横一閃（20）→ 納刀（29）
     */
    private static void dashStorm(ServerPlayer p, int t) {
        if (t == 0) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 0.6F);
        if (t > 2 && t < 10 && t % 3 == 0) chargeFx(p);
        if (t == 10) {
            AttackHelper.lunge(p, 2.1, 0.05);
            AttackHelper.particles(p, ParticleTypes.CLOUD, 10, 0.3, 0.05);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 0.5F);
        }
        if (t >= 12 && t <= 14) {
            AttackHelper.slash(p, 4.0, 160, 0.6F, 0.05F);
            AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.2F + (t - 12) * 0.15F);
        }
        if (t == 17 || t == 20) {
            AttackHelper.slash(p, 4.6, 240, t == 20 ? 1.2F : 0.8F, t == 20 ? 1.0F : 0.2F);
            AttackHelper.sound(p, t == 20 ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        }
        if (t == 29) AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F);
    }

    /** タメ: 左腰の柄の周りに光が集まる */
    private static void chargeFx(ServerPlayer p) {
        Vec3 look = AttackHelper.flatLook(p);
        Vec3 left = new Vec3(look.z, 0, -look.x);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                p.getX() + left.x * 0.35 + look.x * 0.3, p.getY(0.5), p.getZ() + left.z * 0.35 + look.z * 0.3,
                4, 0.2, 0.2, 0.2, 0.05);
    }
}
