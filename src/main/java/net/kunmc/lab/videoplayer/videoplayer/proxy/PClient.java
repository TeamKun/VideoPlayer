package net.kunmc.lab.videoplayer.videoplayer.proxy;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.videoplayer.videoplayer.model.PlayState;
import net.kunmc.lab.videoplayer.videoplayer.model.Quad;
import net.kunmc.lab.videoplayer.videoplayer.mpv.MPlayer;
import net.kunmc.lab.videoplayer.videoplayer.util.Timer;
import net.kunmc.lab.videoplayer.videoplayer.video.VDisplay;
import net.kunmc.lab.videoplayer.videoplayer.video.VDisplayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class PClient implements PCommon {

    private final VDisplayManager manager = new VDisplayManager();

    @Override
    public void registerEvents() {
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void doClientStuff(final FMLClientSetupEvent ev) {
        MPlayer.init();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        Timer.tick();
        MatrixStack stack = event.getMatrixStack();
        manager.render(stack);
    }

    @SubscribeEvent
    public void onTest(ClientChatEvent event) {
        if (event.getMessage().startsWith("#")) {
            event.setCanceled(true);

            String file = StringUtils.substringAfter(event.getMessage(), "#");

            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                Vec3d pos = player.getPositionVec();

                VDisplay display = manager.create(UUID.randomUUID());
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
