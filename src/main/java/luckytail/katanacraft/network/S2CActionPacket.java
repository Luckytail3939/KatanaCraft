package luckytail.katanacraft.network;

import luckytail.katanacraft.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 誰かがモーションを開始した / 弾いた / ガードした（アニメーション再生用） */
public class S2CActionPacket {
    public final int entityId;
    public final int actionId;

    public S2CActionPacket(int entityId, int actionId) {
        this.entityId = entityId;
        this.actionId = actionId;
    }

    public S2CActionPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.actionId = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(actionId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleAction(this));
    }
}
