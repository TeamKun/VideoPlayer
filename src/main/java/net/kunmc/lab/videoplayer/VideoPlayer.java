package net.kunmc.lab.videoplayer;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.IntByReference;
import cz.adamh.utils.NativeUtils;
import net.kunmc.lab.videoplayer.mpv.MpvLibrary;
import net.kunmc.lab.videoplayer.mpv.mpv_event;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("videoplayer")
public class VideoPlayer {

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public VideoPlayer() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private static void check_error(MpvLibrary mpv, int status) throws RuntimeException {
        if (status < 0)
            throw new RuntimeException("mpv API error: " + mpv.mpv_error_string(status));
    }

    private void doClientStuff(final FMLClientSetupEvent ev) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", ev.getMinecraftSupplier().get().gameSettings);

        try {
            NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));
        } catch (IOException e) {
            throw new RuntimeException("JNA Error", e);
        }

        MpvLibrary mpv = MpvLibrary.INSTANCE;
        Pointer ctx = mpv.mpv_create();
        if (ctx == Pointer.NULL)
            throw new RuntimeException("failed creating context");

        check_error(mpv, mpv.mpv_set_option_string(ctx, "input-default-bindings", "yes"));
        mpv.mpv_set_option_string(ctx, "input-vo-keyboard", "yes");
        IntByReference val = new IntByReference();
        val.setValue(1);
        check_error(mpv, mpv.mpv_set_option(ctx, "osc", /*MPV_FORMAT_FLAG = */ 3, val.getPointer()));

        check_error(mpv, mpv.mpv_initialize(ctx));

        StringArray cmd = new StringArray(new String[]{"loadfile", "test.mp4", null});
        check_error(mpv, mpv.mpv_command(ctx, cmd));

        while (true) {
            mpv_event event = mpv.mpv_wait_event(ctx, 10000);
            LOGGER.info("event: " + mpv.mpv_event_name(event.event_id));
            if (event.event_id == /*MPV_EVENT_SHUTDOWN = */ 1)
                break;
       }

        mpv.mpv_terminate_destroy(ctx);

        LOGGER.info(ctx);
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("videoplayer", "helloworld", () -> {
            LOGGER.info("Hello world from the MDK");
            return "Hello world";
        });
    }

    private void processIMC(final InterModProcessEvent event) {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m -> m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }
}
