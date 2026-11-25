package luckytail.katanacraft.client;

import luckytail.katanacraft.Katanacraft;
import luckytail.katanacraft.combat.KatanaMoves;
import luckytail.katanacraft.item.KatanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 自分の姿勢の一時的な変更（クライアント側）。
 * 突進連撃のタメ〜突進の間、しゃがみ姿勢にして視点を下げる。
 * （姿勢はクライアントが毎tick自分で決めるため、サーバーと別にこちらでも設定が必要）
 */
@Mod.EventBusSubscriber(modid = Katanacraft.MODID, value = Dist.CLIENT)
public final class ClientPoseControl {
    private ClientPoseControl() {}

    private static long crouchUntil = -1;
    private static boolean applied;

    /** 突進連撃の開始時に呼ぶ */
    public static void startDashCrouch() {
        crouchUntil = ClientKatanaState.clientTicks + KatanaMoves.DASH_CROUCH_TICKS;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            applied = false;
            crouchUntil = -1;
            return;
        }
        boolean want = ClientKatanaState.clientTicks < crouchUntil && KatanaItem.isHoldingKatana(p);
        if (want) {
            p.setForcedPose(Pose.CROUCHING);
            applied = true;
        } else if (applied) {
            p.setForcedPose(null);
            applied = false;
            crouchUntil = -1;
        }
    }
}
