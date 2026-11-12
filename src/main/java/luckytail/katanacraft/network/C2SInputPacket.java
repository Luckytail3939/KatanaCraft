package luckytail.katanacraft.network;

import luckytail.katanacraft.combat.CombatManager;
import luckytail.katanacraft.combat.InputType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 攻撃入力（左/右 × タップ/長押し） */
public class C2SInputPacket {
    private final InputType input;

    public C2SInputPacket(InputType input) {
        this.input = input;
    }

    public C2SInputPacket(FriendlyByteBuf buf) {
        this.input = InputType.byId(buf.readByte());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(input.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sp = ctx.get().getSender();
        if (sp != null) CombatManager.onInput(sp, input);
    }
}
