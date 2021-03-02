package net.kunmc.lab.videoplayer.videoplayer.proxy;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.videoplayer.videoplayer.model.PlayState;
import net.kunmc.lab.videoplayer.videoplayer.model.Quad;
import net.kunmc.lab.videoplayer.videoplayer.network.PacketContainer;
import net.kunmc.lab.videoplayer.videoplayer.network.PacketDispatcher;
import net.kunmc.lab.videoplayer.videoplayer.patch.VideoPatch;
import net.kunmc.lab.videoplayer.videoplayer.patch.VideoPatchOperation;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public class PCommon {

    public void registerEvents() {
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().register(this);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Packet
        PacketDispatcher.INSTANCE.register();
    }

    @SubscribeEvent
    public void onServerStart(FMLServerStartingEvent event) {
        event.getCommandDispatcher().register(
                Commands.literal("vplayer")
                        .then(Commands.argument("file", StringArgumentType.string())
                                .executes(ctx -> {
                                    String file = StringArgumentType.getString(ctx, "file");

                                    CommandSource player = ctx.getSource();
                                    Vec3d pos = player.getPos();

                                    UUID id = UUID.randomUUID();
                                    Quad quad = new Quad(
                                            player.getEntity().dimension,
                                            pos.add(0, 9, 0),
                                            pos.add(16, 9, 0),
                                            pos.add(16, 0, 0),
                                            pos.add(0, 0, 0)
                                    );
                                    PlayState state = new PlayState();
                                    state.file = file;
                                    state.paused = true;
                                    state.time = 0;

                                    VideoPatch patch = new VideoPatch(id, quad, state);

                                    PacketContainer packet = new PacketContainer(VideoPatchOperation.UPDATE, Collections.singletonList(patch));
                                    ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                                            .map(p -> p.connection)
                                            .filter(Objects::nonNull)
                                            .forEach(p -> PacketDispatcher.INSTANCE.send(p.getNetworkManager(), packet));

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
        );
    }

}
