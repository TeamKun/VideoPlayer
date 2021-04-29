package net.kunmc.lab.vplayer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.kunmc.lab.vplayer.common.model.PlayState;
import net.kunmc.lab.vplayer.common.network.PacketContainer;
import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.common.util.Timer;
import net.kunmc.lab.vplayer.server.command.VPlayerCommand;
import net.kunmc.lab.vplayer.server.network.PacketDispatcherServer;
import net.kunmc.lab.vplayer.server.patch.VideoPatchRecieveEventServer;
import net.kunmc.lab.vplayer.server.patch.VideoPatchSendEventServer;
import net.kunmc.lab.vplayer.server.video.VDisplayManagerServer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProxyServer {

    private static MinecraftServer server;

    public static MinecraftServer getServer() {
        return server;
    }

    public static VDisplayManagerServer getDisplayManager() {
        return VDisplayManagerServer.get(server.overworld());
    }

    public void registerEvents() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Packet
        PacketDispatcherServer.register();
    }

    @SubscribeEvent
    public void onServerStart(FMLServerStartingEvent event) {
        server = event.getServer();
    }

    @SubscribeEvent
    public void onRegisterCommand(RegisterCommandsEvent event) {
        VPlayerCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        Timer.tick();
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        if (!(player instanceof ServerPlayerEntity))
            return;
        VDisplayManagerServer state = getDisplayManager();
        PacketContainer packet = new PacketContainer(VideoPatchOperation.SYNC, state.list().stream()
                .map(p -> new VideoPatch(p.getUUID(), p.getQuad(), p.fetchState())).collect(Collectors.toList()));
        PacketDispatcherServer.send(((ServerPlayerEntity) player).connection.getConnection(), packet);
    }

    @SubscribeEvent
    public void onServerPatchSend(VideoPatchSendEventServer event) {
        if (getServer() == null)
            return;

        PacketContainer packet = new PacketContainer(event.getOperation(), event.getPatches());
        getServer().getPlayerList().getPlayers().stream()
                .map(p -> p.connection)
                .filter(Objects::nonNull)
                .forEach(p -> PacketDispatcherServer.send(p.getConnection(), packet));
    }

    private final Table<UUID, UUID, Double> durationTable = HashBasedTable.create();

    @SubscribeEvent
    public void onServerPatchReceive(VideoPatchRecieveEventServer event) {
        if (event.getOperation() == VideoPatchOperation.UPDATE) {
            VDisplayManagerServer state = getDisplayManager();

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

}
