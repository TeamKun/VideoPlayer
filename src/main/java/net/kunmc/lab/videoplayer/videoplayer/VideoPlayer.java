package net.kunmc.lab.videoplayer.videoplayer;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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

    private final VPlayer playerClient = new VPlayer();

    private void doClientStuff(final FMLClientSetupEvent ev) {
        playerClient.init();
    }

    private Deque<VPlayer.VPlayerClient> addQueue = new ArrayDeque<>();
    private List<VPlayer.VPlayerClient> clients = new ArrayList<>();
    private Deque<VPlayer.VPlayerClient> removeQueue = new ArrayDeque<>();

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        MatrixStack stack = event.getMatrixStack();

        {
            VPlayer.VPlayerClient add;
            while ((add = addQueue.poll()) != null) {
                add.init();
                clients.add(add);
            }
        }

        for (VPlayer.VPlayerClient client : clients) {
            client.onRender(stack);
        }

        {
            VPlayer.VPlayerClient remove;
            while ((remove = removeQueue.poll()) != null) {
                remove.destroy();
            }
        }
    }
}
