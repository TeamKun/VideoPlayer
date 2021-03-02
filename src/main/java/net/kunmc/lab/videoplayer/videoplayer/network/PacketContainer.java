package net.kunmc.lab.videoplayer.videoplayer.network;

import com.google.gson.Gson;
import net.kunmc.lab.videoplayer.videoplayer.VideoPlayer;
import net.kunmc.lab.videoplayer.videoplayer.patch.VideoPatch;
import net.kunmc.lab.videoplayer.videoplayer.patch.VideoPatchEvent;
import net.kunmc.lab.videoplayer.videoplayer.patch.VideoPatchOperation;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Supplier;

public class PacketContainer {

    private static final Gson gson = new Gson();

    private final VideoPatchOperation operation;
    private final List<VideoPatch> patches;

    public PacketContainer(VideoPatchOperation operation, List<VideoPatch> patches) {
        this.operation = operation;
        this.patches = patches;
    }

    public static void encode(PacketContainer message, PacketBuffer buffer) {
        buffer.writeString(gson.toJson(message));
    }

    public static PacketContainer decode(PacketBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.getBytes(0, bytes);
            String messageString = StringUtils.toEncodedString(bytes, StandardCharsets.UTF_8);

            return gson.fromJson(messageString, PacketContainer.class);
        } catch (Exception e) {
            VideoPlayer.LOGGER.warn("Invalid Packet", e);
            return null;
        }
    }

    public static void handle(PacketContainer message, Supplier<NetworkEvent.Context> ctx) {
        if (message == null || message.operation == null)
            return;

        MinecraftForge.EVENT_BUS.post(new VideoPatchEvent(message.operation, message.patches));

        ctx.get().setPacketHandled(true);
    }

}