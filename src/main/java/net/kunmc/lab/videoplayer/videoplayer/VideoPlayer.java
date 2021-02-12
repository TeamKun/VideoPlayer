package net.kunmc.lab.videoplayer.videoplayer;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ClientChatEvent;
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

    @Override
    protected void finalize() throws Throwable {
        playerClient.destroy();
    }

    private Deque<VDisplay> addQueue = new ArrayDeque<>();
    private List<VDisplay> clients = new ArrayList<>();

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        MatrixStack stack = event.getMatrixStack();

        {
            VDisplay add;
            while ((add = addQueue.poll()) != null) {
                add.init(playerClient);
                clients.add(add);
            }
        }

        clients.forEach(client -> client.render(stack));
        clients.removeIf(VDisplay::proceedDestroy);
    }

    @SubscribeEvent
    public void onTest(ClientChatEvent event) {
        if (event.getMessage().startsWith("#"))
            event.setCanceled(true);

        if (event.getMessage().equals("#video")) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                Vec3d pos = player.getPositionVec();
                addQueue.add(new VDisplay(new VQuad(new Vec3d[]{
                        pos.add(0, 1, 0),
                        pos.add(1, 1, 0),
                        pos.add(1, 0, 0),
                        pos.add(0, 0, 0),
                })));
            }
        }
    }
}
