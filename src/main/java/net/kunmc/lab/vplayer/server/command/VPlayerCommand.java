package net.kunmc.lab.vplayer.server.command;

import com.google.common.base.Strings;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kunmc.lab.vplayer.ProxyServer;
import net.kunmc.lab.vplayer.common.model.Display;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.kunmc.lab.vplayer.common.video.VDisplay;
import net.kunmc.lab.vplayer.server.video.VDisplayManagerServer;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ILocationArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class VPlayerCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("vdisplay")
                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();

                                    ITextComponent component = TextComponentUtils.makeList(
                                            state.listNames(),
                                            msg -> {
                                                StringTextComponent text = new StringTextComponent(msg);
                                                Optional.ofNullable(state.get(msg))
                                                        .map(Display::getQuad)
                                                        .ifPresent(e -> text.applyTextStyle(s -> {
                                                            Vec3d vec = e.vertices[0];
                                                            s.setHoverEvent(new HoverEvent(
                                                                    HoverEvent.Action.SHOW_TEXT,
                                                                    new StringTextComponent(String.format("クリックでTP: %s(x:%.1f, y:%.1f, z:%.1f)", msg, vec.x, vec.y, vec.z))
                                                            ));
                                                            s.setClickEvent(new ClickEvent(
                                                                    ClickEvent.Action.RUN_COMMAND,
                                                                    String.format("/tp %f %f %f", vec.x, vec.y, vec.z)
                                                            ));
                                                        }));
                                                return text;
                                            }
                                    );

                                    ctx.getSource().sendFeedback(
                                            new StringTextComponent("").applyTextStyle(TextFormatting.GREEN)
                                                    .appendSibling(new StringTextComponent("[かめすたMod] ").applyTextStyle(TextFormatting.LIGHT_PURPLE))
                                                    .appendSibling(component),
                                            true);

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("create")
                                .then(name()
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                            VDisplay display = state.create(name);

                                            state.setQuad(name, getQuad(ctx, display.getQuad(), ctx.getSource().getPos(), null, true, .1));

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("destroy")
                                .then(name()
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                            state.destroy(name);

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )

                        )
                        .then(Commands.literal("position")
                                .then(name()
                                        .then(Commands.literal("pos1")
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();

                                                    VDisplay display = state.get(name);
                                                    if (display == null)
                                                        throw new CommandException(new StringTextComponent("ディスプレイが見つかりません。"));

                                                    state.setQuad(name, getQuad(ctx, display.getQuad(), ctx.getSource().getPos(), null, true, .1));

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            ILocationArgument pos = Vec3Argument.getLocation(ctx, "pos");

                                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();

                                                            VDisplay display = state.get(name);
                                                            if (display == null)
                                                                throw new CommandException(new StringTextComponent("ディスプレイが見つかりません。"));

                                                            state.setQuad(name, getQuad(ctx, display.getQuad(), pos.getPosition(ctx.getSource()), null, false, .1));

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("pos2")
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();

                                                    VDisplay display = state.get(name);
                                                    if (display == null)
                                                        throw new CommandException(new StringTextComponent("ディスプレイが見つかりません。"));

                                                    state.setQuad(name, getQuad(ctx, display.getQuad(), null, ctx.getSource().getPos(), true, .1));

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                                        .executes(ctx -> {
                                                            String name = StringArgumentType.getString(ctx, "name");
                                                            ILocationArgument pos = Vec3Argument.getLocation(ctx, "pos");

                                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();

                                                            VDisplay display = state.get(name);
                                                            if (display == null)
                                                                throw new CommandException(new StringTextComponent("ディスプレイが見つかりません。"));

                                                            state.setQuad(name, getQuad(ctx, display.getQuad(), null, pos.getPosition(ctx.getSource()), false, .1));

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                )

                        )
        );
        dispatcher.register(
                Commands.literal("vplayer")
                        .then(name()
                                .then(Commands.literal("video")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();

                                            VDisplay display = state.get(name);
                                            if (display == null)
                                                throw new CommandException(new StringTextComponent("ディスプレイが見つかりません。"));

                                            ctx.getSource().sendFeedback(
                                                    new StringTextComponent("").applyTextStyle(TextFormatting.GREEN)
                                                            .appendSibling(new StringTextComponent("[かめすたMod] ").applyTextStyle(TextFormatting.LIGHT_PURPLE))
                                                            .appendSibling(new StringTextComponent(Strings.nullToEmpty(display.fetchState().file))),
                                                    true);

                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("url", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    String url = StringArgumentType.getString(ctx, "url");

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                                    state.dispatchState(name, s -> {
                                                        s.file = url;
                                                        s.time = 0;
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
                                                s.time = 0;
                                                s.paused = false;
                                                return s;
                                            });

                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("url", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    String url = StringArgumentType.getString(ctx, "url");

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                                    state.dispatchState(name, s -> {
                                                        s.file = url;
                                                        s.time = 0;
                                                        s.paused = false;
                                                        return s;
                                                    });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("pause")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                            state.dispatchState(name, s -> {
                                                s.paused = !s.paused;
                                                return s;
                                            });

                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("paused", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    boolean paused = BoolArgumentType.getBool(ctx, "paused");

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                                    state.dispatchState(name, s -> {
                                                        s.paused = paused;
                                                        return s;
                                                    });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("stop")
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");

                                            VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                            state.dispatchState(name, s -> {
                                                s.file = null;
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
                                .then(Commands.literal("time")
                                        .then(Commands.argument("time", VTimeArgumentType.timeArg())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    VTimeArgumentType.VTime time = VTimeArgumentType.getTime(ctx, "time");

                                                    VDisplayManagerServer state = ProxyServer.getDisplayManager();
                                                    state.dispatchState(name, s -> {
                                                        s.time = time.getTime(s.duration);
                                                        return s;
                                                    });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
        );
    }

    @Nonnull
    public static Quad getQuad(CommandContext<CommandSource> context, @Nullable Quad prev, @Nullable Vec3d pos1, @Nullable Vec3d pos2, boolean align, double padding) {
        DimensionType dimension = Optional.ofNullable(prev)
                .map(e -> e.dimension)
                .orElseGet(() ->
                        Optional.ofNullable(context.getSource().getEntity())
                                .map(e -> e.dimension)
                                .orElse(DimensionType.OVERWORLD)
                );

        if (pos1 == null && pos2 == null)
            pos1 = context.getSource().getPos();

        if (prev != null) {
            if (pos1 == null)
                pos1 = prev.vertices[0];
            else if (pos2 == null)
                pos2 = prev.vertices[2];
        } else {
            if (pos1 == null)
                pos1 = pos2.subtract(16, -9, 0);
            else if (pos2 == null)
                pos2 = pos1.add(16, -9, 0);
        }

        double offset = .01;
        if (align) {
            double p1x = pos1.x < pos2.x ? Math.floor(pos1.x) + padding : Math.ceil(pos1.x) - padding;
            double p2x = pos1.x > pos2.x ? Math.floor(pos2.x) + padding : Math.ceil(pos2.x) - padding;
            double p1y = pos1.y < pos2.y ? Math.floor(pos1.y) + padding : Math.ceil(pos1.y) - padding;
            double p2y = pos1.y > pos2.y ? Math.floor(pos2.y) + padding : Math.ceil(pos2.y) - padding;
            double p1z = pos1.z < pos2.z ? Math.floor(pos1.z) + padding : Math.ceil(pos1.z) - padding;
            double p2z = pos1.z > pos2.z ? Math.floor(pos2.z) + padding : Math.ceil(pos2.z) - padding;

            BlockPos b1 = new BlockPos(pos1);
            BlockPos b2 = new BlockPos(pos2);
            if (b1.getX() == b2.getX()) {
                if (pos1.z > pos2.z)
                    p1x = p2x = b1.getX() + offset;
                else
                    p1x = p2x = b1.getX() + (1 - offset);
            } else if (b1.getZ() == b2.getZ()) {
                if (pos1.x < pos2.x)
                    p1z = p2z = b1.getZ() + offset;
                else
                    p1z = p2z = b1.getZ() + (1 - offset);
            }

            pos1 = new Vec3d(p1x, p1y, p1z);
            pos2 = new Vec3d(p2x, p2y, p2z);
        }

        return new Quad(
                dimension,
                new Vec3d(pos1.x, pos1.y, pos1.z),  // left top
                new Vec3d(pos1.x, pos2.y, pos1.z),  // left bottom
                new Vec3d(pos2.x, pos2.y, pos2.z),  // right bottom
                new Vec3d(pos2.x, pos1.y, pos2.z)   // right top
        );
    }

    public static RequiredArgumentBuilder<CommandSource, String> name() {
        return Commands.argument("name", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    VDisplayManagerServer state = ProxyServer.getDisplayManager();

                    state.listNames().forEach(builder::suggest);

                    return builder.buildFuture();
                });
    }
}
