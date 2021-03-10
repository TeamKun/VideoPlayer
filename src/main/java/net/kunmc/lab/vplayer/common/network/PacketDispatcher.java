package net.kunmc.lab.vplayer.common.network;

import com.google.common.base.Suppliers;
import net.kunmc.lab.vplayer.VideoPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PacketDispatcher {

    public static final String PROTOCOL_VERSION = "VP01";
    public static final Predicate<String> VANILLA_OR_HANDSHAKE =
            ((Predicate<String>) NetworkRegistry.ACCEPTVANILLA::equals).or(PROTOCOL_VERSION::equals);

    public static final SimpleChannel channel = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(VideoPlayer.MODID, "patch"))
            .clientAcceptedVersions(VANILLA_OR_HANDSHAKE)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private static Supplier<Void> register = Suppliers.memoize(() -> {
        PacketDispatcher.channel.registerMessage(
                0,
                PacketContainer.class,
                PacketContainer::encode,
                PacketContainer::decode,
                PacketDispatcher::handle
        );
        return null;
    });

    private static BiConsumer<PacketContainer, Supplier<NetworkEvent.Context>> clientHandler;
    private static BiConsumer<PacketContainer, Supplier<NetworkEvent.Context>> serverHandler;

    public static void registerClient(BiConsumer<PacketContainer, Supplier<NetworkEvent.Context>> handler) {
        register.get();
        clientHandler = handler;
    }

    public static void registerServer(BiConsumer<PacketContainer, Supplier<NetworkEvent.Context>> handler) {
        register.get();
        serverHandler = handler;
    }

    private static void handle(PacketContainer message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            if (clientHandler != null)
                clientHandler.accept(message, ctx);
        } else {
            if (serverHandler != null)
                serverHandler.accept(message, ctx);
        }
    }

}