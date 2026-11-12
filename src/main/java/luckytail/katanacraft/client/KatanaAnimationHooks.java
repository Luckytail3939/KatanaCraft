package luckytail.katanacraft.client;

import luckytail.katanacraft.combat.KatanaAction;
import net.minecraft.world.entity.player.Player;

/**
 * アニメーション再生の窓口。
 * サーバーがモーションを開始すると、その刀を振っているプレイヤーと周囲の全員に通知が届き、ここが呼ばれる。
 *
 * 現状はバニラの腕振り（サーバー側の swing）のみ。
 * Player Animation Library (PAL) を導入するときはここだけ差し替えればよい:
 *
 *   1. build.gradle に PAL を追加（例: maven "https://maven.kosmx.dev/" から
 *      dev.kosmx.player-anim:player-animation-lib-forge:1.0.2-rc1+1.20 を fg.deobf で）
 *   2. assets/katanacraft/player_animation/ に KatanaAction 名と対応する json を置く
 *      （light_1.json, light_2.json, light_3.json, flurry.json, heavy.json, dash_storm.json,
 *        art_shiden.json, parry.json, guard_impact.json, guard_broken.json …）
 *   3. ClientModEvents で PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(...) により
 *      プレイヤーごとの ModifierLayer を登録
 *   4. 下の onAction で
 *        var anim = PlayerAnimationRegistry.getAnimation(new ResourceLocation(MODID, action.name().toLowerCase()));
 *        layer.setAnimation(new KeyframeAnimationPlayer(anim));
 *      のように再生する
 */
public final class KatanaAnimationHooks {
    private KatanaAnimationHooks() {}

    public static void onAction(Player player, KatanaAction action) {
        // TODO: Player Animation Library 導入後、ここで action に応じたアニメーションを再生
    }
}
