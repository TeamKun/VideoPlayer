package net.kunmc.lab.videoplayer.videoplayer;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("videoplayer")
public class VideoPlayer {

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public VideoPlayer() {
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private final VideoPlayerClient playerClient = new VideoPlayerClient();
    private boolean initialized = false;

    private void doClientStuff(final FMLClientSetupEvent ev) {
        playerClient.initPlayer();
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (!initialized) {
            initialized = true;

            playerClient.initRenderer();
        }

        playerClient.onRender(event.getMatrixStack());
    }
}
