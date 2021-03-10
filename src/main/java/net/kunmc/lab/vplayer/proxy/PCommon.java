package net.kunmc.lab.vplayer.proxy;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.vplayer.common.model.PlayState;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.kunmc.lab.vplayer.common.network.PacketContainer;
import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.common.util.Timer;
import net.kunmc.lab.vplayer.server.network.PacketDispatcherServer;
import net.kunmc.lab.vplayer.server.patch.VideoPatchRecieveEventServer;
import net.kunmc.lab.vplayer.server.patch.VideoPatchSendEventServer;
import net.kunmc.lab.vplayer.server.video.VDisplayManagerServer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PCommon {

    public void registerEvents() {
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().register(this);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Packet
        PacketDispatcherServer.register();
    }

    private MinecraftServer server;
    private Table<UUID, UUID, Double> durationTable = HashBasedTable.create();

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        Timer.tick();
    }

    @SubscribeEvent
    public void onServerPatchSend(VideoPatchSendEventServer event) {
        if (server == null)
            return;

        PacketContainer packet = new PacketContainer(event.getOperation(), event.getPatches());
        server.getPlayerList().getPlayers().stream()
                .map(p -> p.connection)
                .filter(Objects::nonNull)
                .forEach(p -> PacketDispatcherServer.send(p.getNetworkManager(), packet));
    }

    @SubscribeEvent
    public void onServerPatchReceive(VideoPatchRecieveEventServer event) {
        VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));

        if (event.getOperation() == VideoPatchOperation.UPDATE) {
            event.getPatches().forEach(e -> {
                Optional.ofNullable(state.get(e.getId())).ifPresent(d -> {
                    UUID displayId = d.getUUID();
                    UUID playerId = event.getPlayer().getGameProfile().getId();
                    PlayState newState = e.getState();
                    if (newState != null) {
                        float duration = newState.duration;
                        if (duration >= 0)
                            durationTable.put(displayId, playerId, (double) duration);
                    }
                    durationTable.row(displayId).values().stream()
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                            .entrySet()
                            .stream()
                            .max(Map.Entry.comparingByValue())
                            .ifPresent(f -> {
                                float key = (float) (double) f.getKey();
                                PlayState playState = d.fetchState();
                                if (playState.duration != key) {
                                    playState.duration = key;
                                    d.dispatchState(playState);
                                }
                            });
                });
            });
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        if (!(player instanceof ServerPlayerEntity))
            return;
        VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
        PacketContainer packet = new PacketContainer(VideoPatchOperation.SYNC, state.list().stream().map(p -> new VideoPatch(p.getUUID(), p.getQuad(), p.fetchState())).collect(Collectors.toList()));
        PacketDispatcherServer.send(((ServerPlayerEntity) player).connection.getNetworkManager(), packet);
    }

    @SubscribeEvent
    public void onServerStart(FMLServerStartingEvent event) {
        server = event.getServer();

        event.getCommandDispatcher().register(
                Commands.literal("vplayer")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));

                                    state.listNames().forEach(builder::suggest);

                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("create")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                            state.create(name);

                                            CommandSource player = ctx.getSource();
                                            Vec3d pos = player.getPos();
                                            DimensionType dimension = Optional.ofNullable(player.getEntity())
                                                    .map(e -> e.dimension)
                                                    .orElse(DimensionType.OVERWORLD);

                                            Quad quad = new Quad(
                                                    dimension,
                                                    pos.add(0, 9, 0),
                                                    pos.add(16, 9, 0),
                                                    pos.add(16, 0, 0),
                                                    pos.add(0, 0, 0)
                                            );
                                            state.setQuad(name, quad);

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(Commands.literal("pos")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));

                                            CommandSource player = ctx.getSource();
                                            Vec3d pos = player.getPos();
                                            DimensionType dimension = Optional.ofNullable(player.getEntity())
                                                    .map(e -> e.dimension)
                                                    .orElse(DimensionType.OVERWORLD);

                                            Quad quad = new Quad(
                                                    dimension,
                                                    pos.add(0, 9, 0),
                                                    pos.add(16, 9, 0),
                                                    pos.add(16, 0, 0),
                                                    pos.add(0, 0, 0)
                                            );
                                            state.setQuad(name, quad);

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(Commands.literal("url")
                                        .then(Commands.argument("url", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    String url = StringArgumentType.getString(ctx, "url");

                                                    VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                                    state.dispatchState(name, s -> {
                                                        s.file = url;
                                                        s.paused = true;
                                                        return s;
                                                    });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("play")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                            state.dispatchState(name, s -> {
                                                if (s.duration > 0 && s.time > s.duration)
                                                    s.time = 0;
                                                s.paused = false;
                                                return s;
                                            });

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(Commands.literal("stop")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                            state.dispatchState(name, s -> {
                                                s.paused = true;
                                                return s;
                                            });

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(Commands.literal("seek")
                                        .then(Commands.argument("sec", FloatArgumentType.floatArg())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    float sec = FloatArgumentType.getFloat(ctx, "sec");

                                                    VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                                    state.dispatchState(name, s -> {
                                                        if (s.duration > 0)
                                                            s.time = MathHelper.clamp(MathHelper.clamp(s.time, 0, s.duration) + sec, 0, s.duration);
                                                        else
                                                            s.time += sec;
                                                        return s;
                                                    });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.literal("%")
                                                .then(Commands.argument("%", FloatArgumentType.floatArg())
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            float per = FloatArgumentType.getFloat(ctx, "%");

                                                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                                            state.dispatchState(name, s -> {
                                                                if (s.duration > 0)
                                                                    s.time = s.duration * per / 100f;
                                                                return s;
                                                            });

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("sec")
                                                .then(Commands.argument("sec", FloatArgumentType.floatArg())
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            float sec = FloatArgumentType.getFloat(ctx, "sec");

                                                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                                            state.dispatchState(name, s -> {
                                                                if (s.duration > 0)
                                                                    s.time = MathHelper.clamp(sec, 0, s.duration);
                                                                else
                                                                    s.time = sec;
                                                                return s;
                                                            });

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("destroy")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));
                                            state.destroy(name);

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .executes(ctx -> {
                            VDisplayManagerServer state = VDisplayManagerServer.get(server.getWorld(DimensionType.OVERWORLD));

                            ctx.getSource().sendFeedback(TextComponentUtils.makeList(state.listNames(), StringTextComponent::new), true);

                            return Command.SINGLE_SUCCESS;
                        })
        );
    }

}
