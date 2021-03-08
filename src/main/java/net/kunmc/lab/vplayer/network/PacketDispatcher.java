package net.kunmc.lab.vplayer.network;

import net.kunmc.lab.vplayer.VideoPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Predicate;

public class PacketDispatcher {

    public static final String PROTOCOL_VERSION = "VP01";
    public static final Predicate<String> VANILLA_OR_HANDSHAKE =
            ((Predicate<String>) NetworkRegistry.ACCEPTVANILLA::equals).or(PROTOCOL_VERSION::equals);

    public static final PacketDispatcher INSTANCE = new PacketDispatcher();

    private final SimpleChannel channel;

    public PacketDispatcher() {
        this.channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(VideoPlayer.MODID, "patch"))
                .clientAcceptedVersions(VANILLA_OR_HANDSHAKE)
                .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .simpleChannel();
    }

    public void register() {
        channel.registerMessage(0, PacketContainer.class,
                PacketContainer::encode,
                PacketContainer::decode,
                PacketContainer::handle
        );
    }

    public void send(NetworkManager network, PacketContainer packet) {
        channel.sendTo(packet, network, NetworkDirection.PLAY_TO_CLIENT);
    }

    public void sendToServer(PacketContainer packet) {
        channel.sendToServer(packet);
    }

}