package luckytail.katanacraft.client;

import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.combat.KatanaAction;
import luckytail.katanacraft.item.KatanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 誰がいま抜刀しているか（クライアント側）。
 * 「from tick から until tick まで抜刀」を記録し、
 *  - アイテムモデルの切り替え（katanacraft:drawn）
 *  - 腰の帯刀表示（KatanaHipLayer）
 * に使う。
 *
 * アニメーションの「手が柄を握る瞬間」「刀身が鞘に収まる瞬間」と合わせるため、
 * 下の定数で切り替えのタイミングをずらせる（単位: tick、20tick = 1秒）。
 */
@Mod.EventBusSubscriber(modid = Katanacraft.MODID, value = Dist.CLIENT)
public final class ClientDrawState {
    private ClientDrawState() {}

    /** 納刀状態から始まったとき、手が柄を握る（腰→手に切り替える）までの tick */
    private static int drawDelay(Player player, KatanaAction action) {
        return switch (action) {
            case LIGHT_1, FLURRY -> 2;  // 居合: 柄を握ってすぐ抜く
            case HEAVY -> 13;           // 構えて溜めた後に抜き打ち
            case DASH_STORM -> 10;      // 溜め → 突進の瞬間に抜く
            case ART -> player.getMainHandItem().getItem() instanceof KatanaItem k ? k.getArtDrawTick() : 5;
            default -> 0;
        };
    }

    /** モーションの中で納刀まで行う技の、刀身が鞘に収まる tick（アニメーションに合わせる） */
    private static int sheatheTick(Player player, KatanaAction action) {
        return switch (action) {
            case LIGHT_3, FLURRY -> 15;
            case DASH_STORM -> 29;
            case ART -> player.getMainHandItem().getItem() instanceof KatanaItem k ? k.getArtSheatheTick() : action.duration;
            default -> action.duration;
        };
    }

    /** 納刀アニメーション開始から、刀身が鞘に収まる（手→腰に切り替える）までの tick */
    private static final int SHEATHE_IN_TICKS = 5;
    /** 抜いたまま終わるモーションの後、サーバーから納刀通知が来なかった場合の保険 (ticks) */
    private static final int DRAWN_SAFETY = 30;
    /** 他プレイヤーの弾き・ガードの瞬間に刀を見せる時間 (ticks) */
    private static final int REACTION_DRAW = 10;
    /** 自分がガードを離してから納刀通知が来るまでの保険 (ticks) */
    private static final int GUARD_LINGER = 16;

    private static final class Entry {
        long from;
        long until;
    }

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    private static long now() {
        return ClientKatanaState.clientTicks;
    }

    /** 抜刀を開始（すでに抜いている／抜く予定なら、握るタイミングはそのまま） */
    private static void draw(Player player, int delay, long until, boolean extendOnly) {
        long t = now();
        Entry e = ENTRIES.computeIfAbsent(player.getId(), k -> new Entry());
        boolean active = e.until > t; // 抜刀中、または抜刀予定
        if (!active) e.from = t + delay;
        e.until = extendOnly ? Math.max(e.until, until) : until;
    }

    public static void onAction(Player player, KatanaAction action) {
        long t = now();
        switch (action) {
            // 最後に納刀まで含むモーション: アニメーションで刀身が鞘に収まる tick で腰に戻る
            case LIGHT_3, FLURRY, DASH_STORM, ART ->
                    draw(player, drawDelay(player, action), t + sheatheTick(player, action), false);
            // 抜いたまま終わるモーション: サーバーの納刀通知 (SHEATHE) で鞘に戻る
            case LIGHT_1, LIGHT_2, HEAVY ->
                    draw(player, drawDelay(player, action), t + action.duration + DRAWN_SAFETY, false);
            case SHEATHE -> {
                Entry e = ENTRIES.get(player.getId());
                if (e != null && e.until > t) e.until = t + SHEATHE_IN_TICKS;
            }
            case PARRY, GUARD_IMPACT -> draw(player, 0, t + REACTION_DRAW, true);
            case GUARD_BROKEN, EXHAUSTED, EMPOWERED -> {
            }
        }
    }

    public static boolean isDrawn(LivingEntity entity) {
        long t = now();
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player && ClientKatanaState.guarding) {
            // ガード中は抜刀。離した後もサーバーの納刀通知が来るまで抜いたまま
            Entry e = ENTRIES.computeIfAbsent(entity.getId(), k -> new Entry());
            if (e.until <= t || e.from > t) e.from = t;
            e.until = Math.max(e.until, t + GUARD_LINGER);
            return true;
        }
        Entry e = ENTRIES.get(entity.getId());
        if (e == null) return false;
        if (e.until <= t) {
            ENTRIES.remove(entity.getId());
            return false;
        }
        return e.from <= t;
    }

    /** アニメーション側で「いま抜刀しているか」を知るため */
    public static boolean isDrawnOrDrawing(LivingEntity entity) {
        Entry e = ENTRIES.get(entity.getId());
        return e != null && e.until > now();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ENTRIES.clear();
    }
}
