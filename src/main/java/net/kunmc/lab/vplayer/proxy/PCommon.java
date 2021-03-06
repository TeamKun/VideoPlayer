package net.kunmc.lab.vplayer.proxy;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.model.Quad;
import net.kunmc.lab.vplayer.network.PacketContainer;
import net.kunmc.lab.vplayer.network.PacketDispatcher;
import net.kunmc.lab.vplayer.patch.VideoPatch;
import net.kunmc.lab.vplayer.patch.VideoPatchOperation;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
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
                        .then(Commands.literal("create"))
                        .then(Commands.argument("file", StringArgumentType.string())
                                .executes(ctx -> {
                                    String file = StringArgumentType.getString(ctx, "file");

                                    CommandSource player = ctx.getSource();
                                    Vec3d pos = player.getPos();
                                    DimensionType dimension = Optional.ofNullable(player.getEntity())
                                            .map(e -> e.dimension)
                                            .orElse(DimensionType.OVERWORLD);

                                    UUID id = UUID.randomUUID();
                                    Quad quad = new Quad(
                                            dimension,
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
