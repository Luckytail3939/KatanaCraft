package luckytail.katanacraft.client;

import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** クライアント専用の登録処理（MODバス） */
@Mod.EventBusSubscriber(modid = Katanacraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeys.GUARD);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // 経験値バーの位置に体幹ゲージ（刀を持っている時は経験値バーを非表示にする: ClientForgeEvents）
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "posture", KatanaHud.POSTURE);
        // 画面中央下部のルーン
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "runes", KatanaHud.RUNES);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SLASH_WAVE.get(), SlashWaveRenderer::new);
    }
}
