package luckytail.katanacraft.client;

import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.combat.KatanaAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Player Animation Library (PAL) でプレイヤーの体を動かす。
 *
 * サーバーがモーションを開始すると、本人と周囲のプレイヤー全員に通知が届き、ここでアニメーションを再生する。
 *
 * ■ アニメーションの置き場所と名前
 *   assets/katanacraft/player_animation/*.json
 *   アニメーション名（Blockbench のアニメーション名 / json の "name"）を下の名前にすると自動で使われる。
 *   ファイル名・アニメーション名は小文字のみ。
 *
 *     light_1, light_2, light_3   通常斬撃 1〜3段目（1段目は鞘からの居合）
 *     flurry                       抜刀連撃（左長押し）
 *     heavy                        横薙ぎ（右クリック。タメあり）
 *     dash_storm                   踏み込み乱舞（右長押し）
 *     art_<刀のID>                 固有技（紫電なら art_shiden）
 *     parry                        弾き
 *     guard_impact                 通常ガードで受けた
 *     guard_broken                 ガードを崩された
 *     sheathe                      納刀（1・2段目、横薙ぎ、ガードの後）
 *
 *   すでに抜刀している状態から始まる場合、名前の後ろに "_drawn" を付けたアニメーション
 *   （例: heavy_drawn）があればそちらを優先する。なければ通常版を使う。
 *
 *   アニメーションが存在しない動きは何も再生しない（エラーにはならない）。
 *
 * ■ 一人称視点
 *   PAL の一人称モード（THIRD_PERSON_MODEL）を使い、三人称と同じアニメーションで
 *   腕と刀を一人称視点にも表示する。
 *   一人称専用に動きを作りたい場合は、名前の後ろに "_fp" を付けたアニメーション
 *   （例: light_1_fp）を置くと、自分が一人称視点のときだけそちらが使われる。
 *   （他のプレイヤーからは常に通常版が見える）
 *   "_drawn" と組み合わせる場合は heavy_drawn_fp の順。
 */
@Mod.EventBusSubscriber(modid = Katanacraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KatanaAnimationHooks {
    private KatanaAnimationHooks() {}

    /** プレイヤーごとのアニメーション層のID */
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(Katanacraft.MODID, "katana");
    /** 層の優先度（大きいほど他modのアニメーションより上に重なる） */
    private static final int LAYER_PRIORITY = 1000;
    /** アニメーションを切り替えるときの補間 (ticks) */
    private static final int FADE_TICKS = 2;

    /** 一人称で表示する部位: 右腕, 左腕, 右手の刀, 左手の持ち物 */
    private static final FirstPersonConfiguration FIRST_PERSON_PARTS =
            new FirstPersonConfiguration(true, true, true, false);

    /** 見つからなかったアニメーション名（ログを1回だけ出すため） */
    private static final Set<ResourceLocation> MISSING = new HashSet<>();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // プレイヤーが生成されるたびに、空のアニメーション層を1つ持たせる
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER_ID, LAYER_PRIORITY,
                KatanaAnimationHooks::createLayer);
    }

    private static IAnimation createLayer(AbstractClientPlayer player) {
        return new ModifierLayer<>();
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> layerOf(AbstractClientPlayer player) {
        IAnimation anim = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
        return anim instanceof ModifierLayer<?> layer ? (ModifierLayer<IAnimation>) layer : null;
    }

    /**
     * モーション通知を受けたときに呼ばれる。
     * ClientDrawState が状態を更新する「前」に呼ぶこと（抜刀中かどうかの判定に使うため）。
     */
    public static void onAction(Player player, KatanaAction action) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;
        if (action == KatanaAction.EXHAUSTED || action == KatanaAction.EMPOWERED) return;

        String name = animationName(player, action);
        boolean alreadyDrawn = ClientDrawState.isDrawnOrDrawing(player);
        boolean firstPerson = isLocalFirstPerson(player);

        // 優先順: (_drawn)(_fp) → (_drawn) → (_fp) → 通常版
        KeyframeAnimation anim = null;
        if (alreadyDrawn && firstPerson) anim = find(name + "_drawn_fp", false);
        if (anim == null && alreadyDrawn) anim = find(name + "_drawn", false);
        if (anim == null && firstPerson) anim = find(name + "_fp", false);
        if (anim == null) anim = find(name, true);
        if (anim == null) return;

        ModifierLayer<IAnimation> layer = layerOf(clientPlayer);
        if (layer == null) return;

        KeyframeAnimationPlayer playerAnim = new KeyframeAnimationPlayer(anim)
                .setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL)  // 一人称でも腕と刀を表示
                .setFirstPersonConfiguration(FIRST_PERSON_PARTS);
        layer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(FADE_TICKS, Ease.INOUTSINE),
                playerAnim);
    }

    /** 自分自身を一人称視点で見ているか */
    private static boolean isLocalFirstPerson(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return player == mc.player && mc.options.getCameraType().isFirstPerson();
    }

    private static String animationName(Player player, KatanaAction action) {
        if (action == KatanaAction.ART) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem());
            return "art_" + (id != null ? id.getPath() : "unknown");
        }
        return action.name().toLowerCase(Locale.ROOT);
    }

    private static KeyframeAnimation find(String name, boolean logIfMissing) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Katanacraft.MODID, name);
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(id);
        if (anim == null && logIfMissing && MISSING.add(id)) {
            // まだ作っていないアニメーション。開発中の確認用に1回だけ出力
            System.out.println("[KatanaCraft] player animation not found (skipped): " + id);
        }
        return anim;
    }
}
