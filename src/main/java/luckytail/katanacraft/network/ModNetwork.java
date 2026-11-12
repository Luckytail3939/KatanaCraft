package luckytail.katanacraft.network;

import luckytail.katanacraft.Katanacraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private ModNetwork() {}

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Katanacraft.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(C2SInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SInputPacket::encode).decoder(C2SInputPacket::new)
                .consumerMainThread(C2SInputPacket::handle).add();
        CHANNEL.messageBuilder(C2SGuardPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SGuardPacket::encode).decoder(C2SGuardPacket::new)
                .consumerMainThread(C2SGuardPacket::handle).add();
        CHANNEL.messageBuilder(S2CCombatStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCombatStatePacket::encode).decoder(S2CCombatStatePacket::new)
                .consumerMainThread(S2CCombatStatePacket::handle).add();
        CHANNEL.messageBuilder(S2CActionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CActionPacket::encode).decoder(S2CActionPacket::new)
                .consumerMainThread(S2CActionPacket::handle).add();
    }
}
