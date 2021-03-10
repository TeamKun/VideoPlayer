package net.kunmc.lab.vplayer;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.client.mpv.MPlayer;
import net.kunmc.lab.vplayer.client.network.PacketDispatcherClient;
import net.kunmc.lab.vplayer.client.patch.VideoPatchRecieveEventClient;
import net.kunmc.lab.vplayer.client.patch.VideoPatchSendEventClient;
import net.kunmc.lab.vplayer.client.video.VDisplayManagerClient;
import net.kunmc.lab.vplayer.common.model.Display;
import net.kunmc.lab.vplayer.common.network.PacketContainer;
import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.common.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.List;
import java.util.Optional;

public class ProxyClient extends ProxyServer {

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

}
