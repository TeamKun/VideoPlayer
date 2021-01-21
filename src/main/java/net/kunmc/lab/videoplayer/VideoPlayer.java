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
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

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

    private static final MpvLibrary.get_proc_address get_proc_address = (ctx, name) -> {
        return Pointer.createConstant(GLFW.glfwGetProcAddress(name));
    };

    private static final MpvLibrary.on_wakeup on_wakeup = d -> {
        LOGGER.info("wakeup");
    };

    private static final MpvLibrary.on_render_update on_mpv_redraw = d -> {
        LOGGER.info("render update");
    };

    private void doClientStuff(final FMLClientSetupEvent ev) {
        main();
    }

    public static void main(String... args) {
        try {
            NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));
        } catch (IOException e) {
            throw new RuntimeException("JNA Error", e);
        }

        if (!GLFW.glfwInit())
            throw new RuntimeException("glfw init");

        long window = GLFW.glfwCreateWindow(640, 480, "Hello World", 0, 0);
        if (window == 0) {
            GLFW.glfwTerminate();
            throw new RuntimeException("glfw window error");
        }

        GLFW.glfwMakeContextCurrent(window);

        MpvLibrary mpv = MpvLibrary.INSTANCE;
        long handle = mpv.mpv_create();
        if (handle == 0)
            throw new RuntimeException("failed creating context");

        mpv.mpv_set_option_string(handle, "terminal", "yes");
        mpv.mpv_set_option_string(handle, "msg-level", "all=v");

        // LongByReference longByReference = new LongByReference(Minecraft.getInstance().func_228018_at_().getHeight());
        // mpv.mpv_set_option(handle, "wid", 4, longByReference.getPointer());

        // check_error(mpv, mpv.mpv_set_option_string(handle, "input-default-bindings", "yes"));
        // mpv.mpv_set_option_string(handle, "input-vo-keyboard", "yes");
        // IntByReference val = new IntByReference();
        // val.setValue(1);
        // check_error(mpv, mpv.mpv_set_option(handle, "osc", /*MPV_FORMAT_FLAG = */ 3, val.getPointer()));

        check_error(mpv, mpv.mpv_initialize(handle));

        MpvLibrary.mpv_opengl_init_params gl_init_params = new MpvLibrary.mpv_opengl_init_params();
        gl_init_params.get_proc_address = get_proc_address;
        gl_init_params.get_proc_address_ctx = null;
        gl_init_params.extra_exts = null;
        gl_init_params.write();

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
        check_error(mpv, mpv.mpv_render_context_create(mpv_gl, handle, headParam));

        mpv.mpv_set_wakeup_callback(handle, on_wakeup, null);
        mpv.mpv_render_context_set_update_callback(mpv_gl.getValue(), on_mpv_redraw, null);

        // GL Start

        int fbo = 1;
        int texture;
        int _width = 480;
        int _height = 480;

        GL30.glGenFramebuffers(1, &fbo);
        GL30.glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        glGenTextures(1, &texture);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, _width, _height, 0, GL_RGB, GL_UNSIGNED_BYTE, nullptr);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            printf("Error creating framebuffer\n");
            return 1;
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        int one = 1;
        mpv_opengl_fbo fbo_settings =
                {
                        static_cast<int>(fbo),
                _width,
                _height,
                GL_RGB8
			};
        mpv_render_param render_params[]
        {
            {MPV_RENDER_PARAM_OPENGL_FBO, &fbo_settings},
            {MPV_RENDER_PARAM_FLIP_Y,     &one},
            {MPV_RENDER_PARAM_INVALID,    nullptr}
        };

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(-1.f, 1.f, -1.f, 1.f, -1.0f, 1.0f);

        // GL End

        // Play this file.
	const char* cmd[] = {"loadfile", argv[1], NULL};
        check_error(mpv_command(ctx, cmd));

        // Let it play, and wait until the user quits.
        //	while (1)
        //	{
        //		mpv_event* event = mpv_wait_event(ctx, 10000);
        //		printf("event: %s\n", mpv_event_name(event->event_id));
        //		if (event->event_id == MPV_EVENT_SHUTDOWN)
        //			break;
        //	}

        /* Loop until the user closes the window */
        while (!glfwWindowShouldClose(window))
        {
            glViewport(0, 0, 640, 480);
            /* Render here */
            glClear(GL_COLOR_BUFFER_BIT);

            mpv_render_context_render(mpv_gl, render_params);
            glViewport(0, 0, 640, 480);

            glDisable(GL_TEXTURE_2D);
            glBegin(GL_QUADS);
            glVertex2f(-.75f, -.75f);
            glVertex2f(-.75f, .75f);
            glVertex2f(.75f, .75f);
            glVertex2f(.75f, -.75f);
            glEnd();

            glBindTexture(GL_TEXTURE_2D, texture);
            glEnable(GL_TEXTURE_2D);
            glBegin(GL_QUADS);
            glTexCoord2i(0, 0);
            glVertex2f(-.5f, -.5f);
            glTexCoord2i(0, 1);
            glVertex2f(0.f, .5f);
            glTexCoord2i(1, 1);
            glVertex2f(1.f, .5f);
            glTexCoord2i(1, 0);
            glVertex2f(.5f, -.5f);
            glEnd();
            glDisable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, 0);

            /* Swap front and back buffers */
            glfwSwapBuffers(window);

            /* Poll for and process events */
            glfwPollEvents();
        }

        glfwTerminate();

        //    while (true) {
        //        MpvLibrary.mpv_event event = mpv.mpv_wait_event(handle, 10000);
        //        LOGGER.info("event: " + mpv.mpv_event_name(event.event_id));
        //        if (event.event_id == /*MPV_EVENT_SHUTDOWN = */ 1)
        //            break;
        //    }

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
