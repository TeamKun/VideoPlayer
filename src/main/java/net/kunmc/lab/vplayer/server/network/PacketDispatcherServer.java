package net.kunmc.lab.vplayer.server.network;

import net.kunmc.lab.vplayer.common.network.PacketContainer;
import net.kunmc.lab.vplayer.common.network.PacketDispatcher;
import net.kunmc.lab.vplayer.server.patch.VideoPatchRecieveEventServer;
import net.minecraft.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketDispatcherServer {
    public static void register() {
        PacketDispatcher.registerServer(PacketDispatcherServer::handle);
    }

    public static void handle(PacketContainer message, Supplier<NetworkEvent.Context> ctx) {
        if (message == null || message.getOperation() == null)
            return;

        MinecraftForge.EVENT_BUS.post(new VideoPatchRecieveEventServer(message.getOperation(), message.getPatches(), ctx.get().getSender()));

        ctx.get().setPacketHandled(true);
    }

    public static void send(NetworkManager network, PacketContainer packet) {
        PacketDispatcher.channel.sendTo(packet, network, NetworkDirection.PLAY_TO_CLIENT);
    }
}
