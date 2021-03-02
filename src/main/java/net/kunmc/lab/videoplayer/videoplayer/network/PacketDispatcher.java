package net.kunmc.lab.videoplayer.videoplayer.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class PacketDispatcher {

    private final SimpleChannel channel;

    public PacketDispatcher(ResourceLocation name) {
        this.channel = NetworkRegistry.ChannelBuilder
                .named(name)
                .clientAcceptedVersions(NetworkRegistry.ACCEPTVANILLA::equals)
                .serverAcceptedVersions(NetworkRegistry.ACCEPTVANILLA::equals)
                .networkProtocolVersion(() -> "VP01")
                .simpleChannel();
    }

    public PacketDispatcher register() {
        channel.registerMessage(0, PacketContainer.class,
                PacketContainer::encode,
                PacketContainer::decode,
                PacketContainer::handle
        );

        return this;
    }

}