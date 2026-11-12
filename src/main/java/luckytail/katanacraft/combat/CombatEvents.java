package luckytail.katanacraft.combat;

import luckytail.katanacraft.Config;
import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Katanacraft.MODID)
public final class CombatEvents {
    private CombatEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER
                && event.player instanceof ServerPlayer sp) {
            CombatManager.tick(sp);
        }
    }

    /** 弾き判定（攻撃そのものを無効化） */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && CombatManager.tryParry(sp, event.getSource())) {
            event.setCanceled(true);
        }
    }

    /** 無防備デバフ倍率 → 通常ガード軽減 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        if (target.hasEffect(ModEffects.VULNERABLE.get())) {
            event.setAmount(event.getAmount() * Config.VULNERABLE_MULTIPLIER.get().floatValue());
        }
        if (target instanceof ServerPlayer sp) {
            event.setAmount(CombatManager.onGuardHit(sp, event.getSource(), event.getAmount()));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) CombatManager.remove(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) CombatManager.remove(sp);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) CombatManager.get(sp).dirty = true;
    }
}
