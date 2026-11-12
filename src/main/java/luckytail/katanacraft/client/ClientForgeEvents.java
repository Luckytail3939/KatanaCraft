package luckytail.katanacraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.combat.InputType;
import luckytail.katanacraft.item.KatanaItem;
import luckytail.katanacraft.network.C2SGuardPacket;
import luckytail.katanacraft.network.C2SInputPacket;
import luckytail.katanacraft.network.ModNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 刀を持っている間の入力処理（FORGEバス）。
 *  - 左/右クリックはバニラの攻撃・使用をキャンセルし、タップ/長押しを判定してサーバーへ送る
 *  - ガードキーは押下/離しをそのまま送る（素早いタップでも取りこぼさないようイベントで拾う）
 *  - スニーク中は攻撃入力を奪わない（ブロック破壊やチェストを開く等ができる）
 */
@Mod.EventBusSubscriber(modid = Katanacraft.MODID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {}

    /** これ以上押し続けたら「長押し」 (ticks) */
    private static final int HOLD_TICKS = 7;

    private static final ButtonTracker LIGHT = new ButtonTracker(InputType.LIGHT_TAP, InputType.LIGHT_HOLD);
    private static final ButtonTracker HEAVY = new ButtonTracker(InputType.HEAVY_TAP, InputType.HEAVY_HOLD);
    private static boolean guardSent;

    /** ガード入力を受け付ける状態か */
    private static boolean canGuard(Minecraft mc) {
        LocalPlayer p = mc.player;
        return p != null && mc.screen == null && !p.isSpectator() && KatanaItem.isHoldingKatana(p);
    }

    /** 攻撃入力を刀が奪う状態か */
    private static boolean canAttack(Minecraft mc) {
        return canGuard(mc) && !mc.player.isShiftKeyDown();
    }

    // ---------------- 生の入力イベント ----------------

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_REPEAT) return;
        boolean press = event.getAction() == GLFW.GLFW_PRESS;
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
        if (handleKey(key, press) && press) {
            event.setCanceled(true); // バニラの攻撃/使用/キー状態更新を止める
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_REPEAT) return;
        boolean press = event.getAction() == GLFW.GLFW_PRESS;
        handleKey(InputConstants.getKey(event.getKey(), event.getScanCode()), press);
    }

    /** @return 刀の入力として処理したら true */
    private static boolean handleKey(InputConstants.Key key, boolean press) {
        Minecraft mc = Minecraft.getInstance();
        boolean used = false;

        if (matches(ClientKeys.GUARD, key)) {
            if (press) {
                if (canGuard(mc)) {
                    setGuard(true);
                    used = true;
                }
            } else {
                setGuard(false);
            }
        }
        if (matches(mc.options.keyAttack, key)) {
            if (!press) LIGHT.release();
            else if (canAttack(mc)) {
                LIGHT.press();
                used = true;
            }
        }
        if (matches(mc.options.keyUse, key)) {
            if (!press) HEAVY.release();
            else if (canAttack(mc)) {
                HEAVY.press();
                used = true;
            }
        }
        return used;
    }

    private static boolean matches(KeyMapping mapping, InputConstants.Key key) {
        return !mapping.isUnbound() && mapping.getKey().equals(key);
    }

    private static void setGuard(boolean down) {
        if (down == guardSent) return;
        guardSent = down;
        ModNetwork.CHANNEL.sendToServer(new C2SGuardPacket(down));
    }

    // ---------------- バニラ動作の抑止 ----------------

    /** キーボードに攻撃/使用を割り当てている場合や、押しっぱなしの採掘もここで止める */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if ((event.isAttack() || event.isUseItem()) && canAttack(Minecraft.getInstance())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** 刀を持っている間は経験値バーを隠し、代わりに体幹ゲージを出す */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null && KatanaItem.isHoldingKatana(p)) event.setCanceled(true);
        }
    }

    // ---------------- 毎tick ----------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            LIGHT.reset();
            HEAVY.reset();
            guardSent = false;
            return;
        }
        ClientKatanaState.tick();

        if (!canGuard(mc)) {
            LIGHT.reset();
            HEAVY.reset();
            setGuard(false);
            return;
        }
        if (!canAttack(mc)) {
            LIGHT.reset();
            HEAVY.reset();
            return;
        }
        LIGHT.tick();
        HEAVY.tick();
    }

    /** タップ/長押し判定 */
    private static final class ButtonTracker {
        private final InputType tap;
        private final InputType hold;
        private boolean down;
        private boolean holdSent;
        private int ticks;

        ButtonTracker(InputType tap, InputType hold) {
            this.tap = tap;
            this.hold = hold;
        }

        void press() {
            if (down) return;
            down = true;
            holdSent = false;
            ticks = 0;
        }

        void release() {
            if (!down) return;
            down = false;
            if (!holdSent) send(tap);
        }

        void tick() {
            if (!down) return;
            ticks++;
            if (!holdSent && ticks >= HOLD_TICKS) {
                holdSent = true;
                send(hold);
            }
        }

        void reset() {
            down = false;
            holdSent = false;
            ticks = 0;
        }

        private static void send(InputType type) {
            ModNetwork.CHANNEL.sendToServer(new C2SInputPacket(type));
        }
    }
}
