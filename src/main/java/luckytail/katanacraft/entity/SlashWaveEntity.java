package luckytail.katanacraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

/**
 * 光波斬撃。まっすぐ飛び、通過した敵すべてに1回ずつダメージを与える（貫通）。
 * ブロックに当たるか寿命で消える。
 */
public class SlashWaveEntity extends Projectile {
    public static final int LIFETIME = 28;
    public static final double SPEED = 1.8;   // 1tickあたり → 射程 約50ブロック
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.72F, 0.45F, 1.0F), 1.6F);

    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(SlashWaveEntity.class, EntityDataSerializers.FLOAT);

    private float damage = 8.0F;
    private float cachedScale; // 当たり判定サイズ計算用（0 = 未設定 → 1倍）
    private final Set<Integer> hitEntities = new HashSet<>();

    public SlashWaveEntity(EntityType<? extends SlashWaveEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SCALE, 1.0F);
    }

    /** 光波の大きさ（見た目・当たり判定の両方に効く）。スポーン前に設定する */
    public void setWaveScale(float scale) {
        this.entityData.set(SCALE, scale);
        this.cachedScale = scale;
        refreshDimensions();
    }

    public float getWaveScale() {
        return cachedScale <= 0 ? 1.0F : cachedScale;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SCALE.equals(key)) {
            this.cachedScale = this.entityData.get(SCALE);
            refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getWaveScale());
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();

        if (!level().isClientSide) {
            if (tickCount > LIFETIME) {
                discard();
                return;
            }
            hitAlongPath(motion);

            BlockPos next = BlockPos.containing(position().add(motion).add(0, 0.6, 0));
            if (!level().getBlockState(next).getCollisionShape(level(), next).isEmpty()) {
                ServerLevel sl = (ServerLevel) level();
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.6, getZ(), 30, 0.8, 0.3, 0.8, 0.3);
                sl.sendParticles(PURPLE, getX(), getY() + 0.6, getZ(), 20, 0.8, 0.3, 0.8, 0.0);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4F, 1.8F);
                discard();
                return;
            }
        } else {
            spawnTrail(motion);
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
    }

    private void hitAlongPath(Vec3 motion) {
        Entity owner = getOwner();
        AABB sweep = getBoundingBox().expandTowards(motion).inflate(0.3);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, sweep,
                t -> t != owner && t.isAlive() && !t.isSpectator() && !hitEntities.contains(t.getId())
                        && (owner == null || !owner.isAlliedTo(t))
                        && !(t instanceof TamableAnimal ta && owner instanceof LivingEntity ol && ta.isOwnedBy(ol)))) {
            hitEntities.add(e.getId());
            e.invulnerableTime = 0;
            DamageSource src = damageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null);
            if (e.hurt(src, damage)) {
                Vec3 dir = motion.normalize();
                e.knockback(0.5, -dir.x, -dir.z);
                ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        e.getX(), e.getY(0.6), e.getZ(), 12, 0.2, 0.3, 0.2, 0.3);
            }
        }
    }

    /** 進行方向に垂直な弧状にパーティクルを並べる（クライアント側） */
    private void spawnTrail(Vec3 motion) {
        Vec3 fwd = motion.lengthSqr() > 1.0E-6 ? motion.normalize() : new Vec3(0, 0, 1);
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        if (side.lengthSqr() < 1.0E-6) side = new Vec3(1, 0, 0);
        side = side.normalize();
        float sc = getWaveScale();
        int count = sc > 1.2F ? 8 : 5;
        for (int i = -count; i <= count; i++) {
            double f = (double) i / count;
            double bulge = (1.0 - f * f) * 0.7 * sc;
            double x = getX() + side.x * f * 2.0 * sc + fwd.x * bulge;
            double y = getY() + 0.6 * sc + fwd.y * bulge;
            double z = getZ() + side.z * f * 2.0 * sc + fwd.z * bulge;
            level().addParticle(PURPLE, x, y, z, 0, 0, 0);
            if (random.nextInt(4) == 0) level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("Scale", getWaveScale());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        if (tag.contains("Scale")) setWaveScale(tag.getFloat("Scale"));
    }

    @Override
    public boolean shouldBeSaved() {
        return false; // 一瞬で消えるのでセーブしない
    }
}
