package net.kunmc.lab.videoplayer;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import cz.adamh.utils.NativeUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
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

    private static final MpvLibrary.get_proc_address get_proc_address = (handle1, name) -> LOGGER.info("name: " + name.getString(0));

    private void doClientStuff(final FMLClientSetupEvent ev) {
        main();
    }

    public static void main(String... args) {
        try {
            NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));
        } catch (IOException e) {
            throw new RuntimeException("JNA Error", e);
        }

        MpvLibrary mpv = MpvLibrary.INSTANCE;
        long handle = mpv.mpv_create();
        if (handle == 0)
            throw new RuntimeException("failed creating context");

        // LongByReference longByReference = new LongByReference(Minecraft.getInstance().func_228018_at_().getHeight());
        // mpv.mpv_set_option(handle, "wid", 4, longByReference.getPointer());

        // check_error(mpv, mpv.mpv_set_option_string(handle, "input-default-bindings", "yes"));
        // mpv.mpv_set_option_string(handle, "input-vo-keyboard", "yes");
        // IntByReference val = new IntByReference();
        // val.setValue(1);
        // check_error(mpv, mpv.mpv_set_option(handle, "osc", /*MPV_FORMAT_FLAG = */ 3, val.getPointer()));

        check_error(mpv, mpv.mpv_initialize(handle));
        check_error(mpv, mpv.mpv_command(handle, new String[]{"loadfile", "test.mp4"}));

        MpvLibrary.mpv_opengl_init_params gl_init_params = new MpvLibrary.mpv_opengl_init_params();
        gl_init_params.get_proc_address = get_proc_address;
        gl_init_params.get_proc_address_ctx = null;
        gl_init_params.extra_exts = null;

        String MPV_RENDER_API_TYPE_OPENGL_STR = "opengl";
        Pointer MPV_RENDER_API_TYPE_OPENGL = new Memory(MPV_RENDER_API_TYPE_OPENGL_STR.getBytes().length + 1);
        MPV_RENDER_API_TYPE_OPENGL.setString(0, MPV_RENDER_API_TYPE_OPENGL_STR);

        MpvLibrary.mpv_render_param headParam = new MpvLibrary.mpv_render_param();
        MpvLibrary.mpv_render_param[] params = (MpvLibrary.mpv_render_param[]) headParam.toArray(3);
        params[0].type = MpvLibrary.MPV_RENDER_PARAM_API_TYPE;
        params[0].data = MPV_RENDER_API_TYPE_OPENGL;
        params[0].write();
        params[1].type = MpvLibrary.MPV_RENDER_PARAM_OPENGL_INIT_PARAMS;
        params[1].data = gl_init_params.getPointer();
        params[1].write();
        params[2].type = MpvLibrary.MPV_RENDER_PARAM_INVALID;
        params[2].data = null;
        params[2].write();

        PointerByReference mpv_gl = new PointerByReference();
        mpv_gl.setValue(null);

        // check_error(mpv, mpv.mpv_render_context_create(mpv_gl.getPointer(), handle, param));
        check_error(mpv, mpv.mpv_render_context_create(mpv_gl.getPointer(), handle, headParam.getPointer()));

        while (true) {
            MpvLibrary.mpv_event event = mpv.mpv_wait_event(handle, 10000);
            LOGGER.info("event: " + mpv.mpv_event_name(event.event_id));
            if (event.event_id == /*MPV_EVENT_SHUTDOWN = */ 1)
                break;
        }

        mpv.mpv_terminate_destroy(handle);

        LOGGER.info(handle);
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
