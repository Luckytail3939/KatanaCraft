package luckytail.katanacraft.client;

import luckytail.katanacraft.combat.KatanaAction;
import luckytail.katanacraft.network.S2CActionPacket;
import luckytail.katanacraft.network.S2CCombatStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** クライアント側のパケット処理（サーバーではロードされない） */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handleState(S2CCombatStatePacket packet) {
        ClientKatanaState.apply(packet);
    }

    public static void handleAction(S2CActionPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(packet.entityId);
        if (e instanceof Player player) {
            KatanaAction action = KatanaAction.byId(packet.actionId);
            if (player == mc.player && action == KatanaAction.EXHAUSTED) {
                ClientKatanaState.exhaustedTime = ClientKatanaState.clientTicks;
            }
            KatanaAnimationHooks.onAction(player, action); // 先に再生（抜刀中かどうかを見るため）
            ClientDrawState.onAction(player, action);
            ClientSlashFx.onAction(player, action);
            if (player == mc.player && action == KatanaAction.DASH_STORM) {
                ClientPoseControl.startDashCrouch();
            }
        }
    }
}
