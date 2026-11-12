package luckytail.katanacraft.network;

import luckytail.katanacraft.combat.CombatManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** ガードキーの押下/離し */
public class C2SGuardPacket {
    private final boolean down;

    public C2SGuardPacket(boolean down) {
        this.down = down;
    }

    public C2SGuardPacket(FriendlyByteBuf buf) {
        this.down = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(down);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sp = ctx.get().getSender();
        if (sp != null) CombatManager.onGuardInput(sp, down);
    }
}
