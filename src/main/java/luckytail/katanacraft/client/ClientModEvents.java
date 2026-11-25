package luckytail.katanacraft.client;

import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.item.KatanaItem;
import luckytail.katanacraft.registry.ModEntities;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** クライアント専用の登録処理（MODバス） */
@Mod.EventBusSubscriber(modid = Katanacraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 抜刀中は "katanacraft:drawn" = 1 → models/item/<刀>.json の overrides で抜刀モデルに切り替わる
            ResourceLocation drawn = ResourceLocation.fromNamespaceAndPath(Katanacraft.MODID, "drawn");
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (item instanceof KatanaItem) {
                    ItemProperties.register(item, drawn,
                            (stack, level, entity, seed) -> entity != null && ClientDrawState.isDrawn(entity) ? 1F : 0F);
                }
            }
        });
    }

    /** 腰に差す用の3Dモデルを読み込ませる（アイテムとしては直接参照されないため） */
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof KatanaItem katana) event.register(katana.getHipModelLocation());
        }
    }

    /** プレイヤーの描画に「帯刀」レイヤーを追加（通常体型・細身体型の両方） */
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new KatanaHipLayer(renderer));
        }
    }

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
