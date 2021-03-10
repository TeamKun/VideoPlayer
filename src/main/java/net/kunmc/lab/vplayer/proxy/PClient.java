package net.kunmc.lab.vplayer.proxy;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.client.mpv.MPlayer;
import net.kunmc.lab.vplayer.client.network.PacketDispatcherClient;
import net.kunmc.lab.vplayer.client.patch.VideoPatchRecieveEventClient;
import net.kunmc.lab.vplayer.client.patch.VideoPatchSendEventClient;
import net.kunmc.lab.vplayer.client.video.VDisplayManagerClient;
import net.kunmc.lab.vplayer.common.model.Display;
import net.kunmc.lab.vplayer.common.model.PlayState;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.kunmc.lab.vplayer.common.network.PacketContainer;
import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.common.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PClient extends PCommon {

    private final VDisplayManagerClient manager = new VDisplayManagerClient();

    @Override
    public void registerEvents() {
        super.registerEvents();

        // Packet
        PacketDispatcherClient.register();
    }

    @SubscribeEvent
    public void doClientStuff(final FMLClientSetupEvent ev) {
        MPlayer.init();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        Timer.tick();
        MatrixStack stack = event.getMatrixStack();
        manager.render(stack);
    }

    @SubscribeEvent
    public void onDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        manager.clear();
    }

    @SubscribeEvent
    public void onClientPatchSend(VideoPatchSendEventClient event) {
        if (Minecraft.getInstance().getConnection() == null)
            return;

        PacketContainer packet = new PacketContainer(event.getOperation(), event.getPatches());
        PacketDispatcherClient.sendToServer(packet);
    }

    @SubscribeEvent
    public void onClientPatchReceive(VideoPatchRecieveEventClient event) {
        VideoPatchOperation op = event.getOperation();
        List<VideoPatch> patches = event.getPatches();

        switch (op) {
            case SYNC:
                manager.clear();
                patches.forEach(p -> {
                    Display display = manager.create(p.getId());
                    display.setQuad(p.getQuad());
                    Optional.ofNullable(p.getState()).ifPresent(display::dispatchState);
                });
                break;
            case UPDATE:
                patches.forEach(p -> {
                    Display display = manager.computeIfAbsent(p.getId());
                    display.setQuad(p.getQuad());
                    Optional.ofNullable(p.getState()).ifPresent(display::dispatchState);
                });
                break;
            case DELETE:
                patches.forEach(p -> manager.destroy(p.getId()));
                break;
        }
    }

    @SubscribeEvent
    public void onTest(ClientChatEvent event) {
        if (event.getMessage().startsWith("#")) {
            event.setCanceled(true);

            String file = StringUtils.substringAfter(event.getMessage(), "#");

            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                Vec3d pos = player.getPositionVec();

                Display display = manager.create(UUID.randomUUID());
                display.setQuad(new Quad(
                        player.dimension,
                        pos.add(0, 9, 0),
                        pos.add(16, 9, 0),
                        pos.add(16, 0, 0),
                        pos.add(0, 0, 0)
                ));
                PlayState state = new PlayState();
                state.file = file;
                state.paused = true;
                state.time = 0;
                display.dispatchState(state);
            }
        }

        if (event.getMessage().startsWith("!")) {
            event.setCanceled(true);

            String command = StringUtils.substringAfter(event.getMessage(), "!");
            switch (command) {
                case "<":
                    manager.list().forEach(e -> {
                        PlayState state = e.fetchState();
                        state.time -= 5;
                        e.dispatchState(state);
                    });
                    break;
                case ">":
                    manager.list().forEach(e -> {
                        PlayState state = e.fetchState();
                        state.time += 5;
                        e.dispatchState(state);
                    });
                    break;
                case "=":
                    manager.list().forEach(e -> {
                        PlayState state = e.fetchState();
                        state.paused = !state.paused;
                        e.dispatchState(state);
                    });
                    break;
                case "x":
                    manager.clear();
                    break;
            }
        }
    }

}
