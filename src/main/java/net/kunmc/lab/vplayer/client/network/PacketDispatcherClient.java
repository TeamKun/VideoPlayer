package net.kunmc.lab.vplayer.client.network;

import net.kunmc.lab.vplayer.client.patch.VideoPatchRecieveEventClient;
import net.kunmc.lab.vplayer.common.network.PacketContainer;
import net.kunmc.lab.vplayer.common.network.PacketDispatcher;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketDispatcherClient {
    public static void register() {
        PacketDispatcher.registerClient(PacketDispatcherClient::handle);
    }

    public static void handle(PacketContainer message, Supplier<NetworkEvent.Context> ctx) {
        if (message == null || message.getOperation() == null)
            return;

        MinecraftForge.EVENT_BUS.post(new VideoPatchRecieveEventClient(message.getOperation(), message.getPatches()));

        ctx.get().setPacketHandled(true);
    }

    public static void sendToServer(PacketContainer packet) {
        PacketDispatcher.channel.sendToServer(packet);
    }
}
