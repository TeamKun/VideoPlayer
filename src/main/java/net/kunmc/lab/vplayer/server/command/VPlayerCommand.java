package net.kunmc.lab.vplayer.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.kunmc.lab.vplayer.ProxyServer;
import net.kunmc.lab.vplayer.server.video.VDisplayManagerServer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.world.dimension.DimensionType;

import java.util.Optional;

public class VPlayerCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("vplayer")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();

                                    state.listNames().forEach(builder::suggest);

                                    return builder.buildFuture();
                                })
                                .then(Commands.literal("create")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
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

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();

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

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();
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

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
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

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
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

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();
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

                                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
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

                                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
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

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                            state.destroy(name);

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .executes(ctx -> {
                            VDisplayManagerServer state = ProxyServer.getDisplayManager();

                            ctx.getSource().sendFeedback(TextComponentUtils.makeList(state.listNames(), StringTextComponent::new), true);

                            return Command.SINGLE_SUCCESS;
                        })
        );
    }
}
