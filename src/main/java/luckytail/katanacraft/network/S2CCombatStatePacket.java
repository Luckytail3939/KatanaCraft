package luckytail.katanacraft.network;

import luckytail.katanacraft.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 自分の体幹・ガード・ルーン状態（HUD用） */
public class S2CCombatStatePacket {
    public final float posture;
    public final float maxPosture;
    public final boolean guarding;
    public final byte[] runes;
    public final int sequenceLength;
    public final int runeTimer;
    public final int artCooldown;
    public final int artCooldownMax;
    public final boolean artFired;
    public final int parryStacks;
    public final int parriesToEmpower;
    public final int empoweredTicks;
    public final int empowerDuration;

    public S2CCombatStatePacket(float posture, float maxPosture, boolean guarding, byte[] runes, int sequenceLength,
                                int runeTimer, int artCooldown, int artCooldownMax, boolean artFired,
                                int parryStacks, int parriesToEmpower, int empoweredTicks, int empowerDuration) {
        this.posture = posture;
        this.maxPosture = maxPosture;
        this.guarding = guarding;
        this.runes = runes;
        this.sequenceLength = sequenceLength;
        this.runeTimer = runeTimer;
        this.artCooldown = artCooldown;
        this.artCooldownMax = artCooldownMax;
        this.artFired = artFired;
        this.parryStacks = parryStacks;
        this.parriesToEmpower = parriesToEmpower;
        this.empoweredTicks = empoweredTicks;
        this.empowerDuration = empowerDuration;
    }

    public S2CCombatStatePacket(FriendlyByteBuf buf) {
        this.posture = buf.readFloat();
        this.maxPosture = buf.readFloat();
        this.guarding = buf.readBoolean();
        this.runes = buf.readByteArray(64);
        this.sequenceLength = buf.readVarInt();
        this.runeTimer = buf.readVarInt();
        this.artCooldown = buf.readVarInt();
        this.artCooldownMax = buf.readVarInt();
        this.artFired = buf.readBoolean();
        this.parryStacks = buf.readVarInt();
        this.parriesToEmpower = buf.readVarInt();
        this.empoweredTicks = buf.readVarInt();
        this.empowerDuration = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(posture);
        buf.writeFloat(maxPosture);
        buf.writeBoolean(guarding);
        buf.writeByteArray(runes);
        buf.writeVarInt(sequenceLength);
        buf.writeVarInt(runeTimer);
        buf.writeVarInt(artCooldown);
        buf.writeVarInt(artCooldownMax);
        buf.writeBoolean(artFired);
        buf.writeVarInt(parryStacks);
        buf.writeVarInt(parriesToEmpower);
        buf.writeVarInt(empoweredTicks);
        buf.writeVarInt(empowerDuration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleState(this));
    }
}
