package net.kunmc.lab.vplayer.network;

import net.kunmc.lab.vplayer.VideoPlayer;
import net.kunmc.lab.vplayer.data.DataSerializer;
import net.kunmc.lab.vplayer.patch.VideoPatch;
import net.kunmc.lab.vplayer.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.patch.VideoPatchOperation;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketContainer {

    private final VideoPatchOperation operation;
    private final List<VideoPatch> patches;

    public PacketContainer(VideoPatchOperation operation, List<VideoPatch> patches) {
        this.operation = operation;
        this.patches = patches;
    }

    public static void encode(PacketContainer message, PacketBuffer buffer) {
        buffer.writeString(DataSerializer.encode(message));
    }

    public static PacketContainer decode(PacketBuffer buffer) {
        PacketContainer data = DataSerializer.decode(buffer.readString(), PacketContainer.class);
        if (data == null)
            VideoPlayer.LOGGER.warn("Invalid Packet");
        return data;
    }

    public static void handle(PacketContainer message, Supplier<NetworkEvent.Context> ctx) {
        if (message == null || message.operation == null)
            return;

        if (ctx.get().getDirection().getReceptionSide().isClient())
            MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Client.ReceiveFromServer(message.operation, message.patches));
        else
            MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Server.ReceiveFromClient(message.operation, message.patches, ctx.get().getSender()));

        ctx.get().setPacketHandled(true);
    }

}