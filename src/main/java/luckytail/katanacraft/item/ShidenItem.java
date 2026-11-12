package luckytail.katanacraft.item;

import luckytail.katanacraft.combat.AttackHelper;
import luckytail.katanacraft.combat.InputType;
import luckytail.katanacraft.entity.SlashWaveEntity;
import luckytail.katanacraft.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 刀「紫電」
 * 固有技「紫電一閃」: 後方へバックステップし、長射程の光波斬撃を放つ。
 * ルーン: 左 → 左 → 右 → 右
 * 強化時: 光波が大きくなり、ダメージも上がる
 */
public class ShidenItem extends KatanaItem {

    private static final List<InputType> ART_SEQUENCE = List.of(
            InputType.LIGHT_TAP, InputType.LIGHT_TAP, InputType.HEAVY_TAP, InputType.HEAVY_TAP);

    /** 強化時の光波の大きさ倍率 */
    private static final float EMPOWERED_SCALE = 1.9F;
    /** 強化時のダメージ倍率 */
    private static final float EMPOWERED_DAMAGE = 1.6F;

    public ShidenItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public List<InputType> getArtSequence() {
        return ART_SEQUENCE;
    }

    @Override
    public int getArtDuration() {
        return 22;
    }

    @Override
    public void artTick(ServerPlayer p, int tick, boolean empowered) {
        switch (tick) {
            case 0 -> {
                // バックステップ
                AttackHelper.lunge(p, -1.25, 0.32);
                AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 0.6F);
                AttackHelper.particles(p, ParticleTypes.CLOUD, 6, 0.3, 0.02);
            }
            case 4 -> {
                AttackHelper.sound(p, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.8F, empowered ? 1.2F : 1.6F);
                if (empowered) AttackHelper.particles(p, ParticleTypes.ELECTRIC_SPARK, 25, 0.6, 0.3);
            }
            case 7 -> {
                // 光波斬撃を放つ
                SlashWaveEntity wave = new SlashWaveEntity(ModEntities.SLASH_WAVE.get(), p.level());
                wave.setOwner(p);
                float base = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
                wave.setDamage(base * 2.2F * (empowered ? EMPOWERED_DAMAGE : 1F));
                wave.setWaveScale(empowered ? EMPOWERED_SCALE : 1F);
                Vec3 look = p.getLookAngle();
                wave.setPos(p.getX() + look.x * 0.8, p.getY() + 0.35, p.getZ() + look.z * 0.8);
                wave.setDeltaMovement(look.scale(SlashWaveEntity.SPEED));
                wave.setYRot(p.getYRot());
                wave.setXRot(p.getXRot());
                wave.yRotO = p.getYRot();
                wave.xRotO = p.getXRot();
                p.level().addFreshEntity(wave);

                p.swing(InteractionHand.MAIN_HAND, true);
                AttackHelper.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);
                AttackHelper.sound(p, SoundEvents.TRIDENT_RIPTIDE_1, 0.7F, 1.5F);
                AttackHelper.sweepFx(p, 1.6, 3);
                if (empowered) {
                    AttackHelper.sound(p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.4F);
                    AttackHelper.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.6F);
                }
            }
            case 18 -> AttackHelper.sound(p, SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.8F); // 納刀
            default -> {
            }
        }
    }
}
