package net.kunmc.lab.videoplayer;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import cz.adamh.utils.NativeUtils;
import net.kunmc.lab.videoplayer.videoplayer.MpvLibrary;
import net.kunmc.lab.videoplayer.videoplayer.VPlayer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.ByteBuffer;

import static net.kunmc.lab.videoplayer.videoplayer.VPlayer.check_error;
import static net.kunmc.lab.videoplayer.videoplayer.VideoPlayer.LOGGER;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

public class VideoPlayerTest {
    public static void main(String... args) {
        try {
            NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));
        } catch (IOException e) {
            throw new RuntimeException("JNA Error", e);
        }

        if (!glfwInit())
            throw new RuntimeException("glfw init");

        long window = glfwCreateWindow(640, 480, "Hello World", 0, 0);
        if (window == 0) {
            glfwTerminate();
            throw new RuntimeException("glfw window error");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        VPlayer playerClient = new VPlayer();

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
        gl_init_params.get_proc_address = playerClient.get_proc_address;
        gl_init_params.get_proc_address_ctx = null;
        gl_init_params.extra_exts = null;
        gl_init_params.write();

        String MPV_RENDER_API_TYPE_OPENGL_STR = "opengl";
        Pointer MPV_RENDER_API_TYPE_OPENGL = new Memory(MPV_RENDER_API_TYPE_OPENGL_STR.getBytes().length + 1);
        MPV_RENDER_API_TYPE_OPENGL.setString(0, MPV_RENDER_API_TYPE_OPENGL_STR);

        MpvLibrary.mpv_render_param head_init_param = new MpvLibrary.mpv_render_param();
        MpvLibrary.mpv_render_param[] init_params = (MpvLibrary.mpv_render_param[]) head_init_param.toArray(3);
        init_params[0].type = MpvLibrary.MPV_RENDER_PARAM_API_TYPE;
        init_params[0].data = MPV_RENDER_API_TYPE_OPENGL;
        init_params[0].write();
        init_params[1].type = MpvLibrary.MPV_RENDER_PARAM_OPENGL_INIT_PARAMS;
        init_params[1].data = gl_init_params.getPointer();
        init_params[1].write();
        init_params[2].type = MpvLibrary.MPV_RENDER_PARAM_INVALID;
        init_params[2].data = null;
        init_params[2].write();

        PointerByReference mpv_gl = new PointerByReference();
        mpv_gl.setValue(null);

        // check_error(mpv, mpv.mpv_render_context_create(mpv_gl.getPointer(), handle, param));
        check_error(mpv, mpv.mpv_render_context_create(mpv_gl, handle, head_init_param));

        VPlayer.VPlayerClient vclient = playerClient.new VPlayerClient();

        // mpv.mpv_set_wakeup_callback(handle, vclient.on_wakeup, null);
        mpv.mpv_render_context_set_update_callback(mpv_gl.getValue(), vclient.on_mpv_redraw, null);

        // GL Start

        int _width = 480;
        int _height = 480;

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        int texture = GL11.glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, _width, _height, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Error creating framebuffer");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        MpvLibrary.mpv_opengl_fbo fbo_settings = new MpvLibrary.mpv_opengl_fbo();
        fbo_settings.fbo = fbo;
        fbo_settings.w = _width;
        fbo_settings.h = _height;
        fbo_settings.internal_format = GL_RGB8;
        fbo_settings.write();

        IntByReference one = new IntByReference(1);

        MpvLibrary.mpv_render_param head_render_param = new MpvLibrary.mpv_render_param();
        MpvLibrary.mpv_render_param[] render_params = (MpvLibrary.mpv_render_param[]) head_render_param.toArray(3);
        render_params[0].type = MpvLibrary.MPV_RENDER_PARAM_OPENGL_FBO;
        render_params[0].data = fbo_settings.getPointer();
        render_params[0].write();
        render_params[1].type = MpvLibrary.MPV_RENDER_PARAM_FLIP_Y;
        render_params[1].data = one.getPointer();
        render_params[1].write();
        render_params[2].type = MpvLibrary.MPV_RENDER_PARAM_INVALID;
        render_params[2].data = null;
        render_params[2].write();

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(-1.f, 1.f, -1.f, 1.f, -1.0f, 1.0f);

        // GL End

        // Play this file.
        check_error(mpv, mpv.mpv_command(handle, new String[]{"loadfile", "test.mp4", null}));

        // Let it play, and wait until the user quits.
        //	while (1)
        //	{
        //		mpv_event* event = mpv_wait_event(ctx, 10000);
        //		printf("event: %s\n", mpv_event_name(event->event_id));
        //		if (event->event_id == MPV_EVENT_SHUTDOWN)
        //			break;
        //	}

        /* Loop until the user closes the window */
        while (!glfwWindowShouldClose(window)) {
            glViewport(0, 0, 640, 480);
            /* Render here */
            glClear(GL_COLOR_BUFFER_BIT);

            mpv.mpv_render_context_render(mpv_gl.getValue(), head_render_param);
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

/*
            LOGGER.info("GL_DITHER: " + glIsEnabled(GL_DITHER));

            int[] viewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, viewport);
            LOGGER.info("GL_VIEWPORT: " + String.format("x:%d, y:%d, w:%d, h:%d", viewport[0], viewport[1], viewport[2], viewport[3]));

            int[] scissor = new int[4];
            glGetIntegerv(GL_SCISSOR_BOX, scissor);
            LOGGER.info("GL_SCISSOR_BOX: " + String.format("x:%d, y:%d, w:%d, h:%d", scissor[0], scissor[1], scissor[2], scissor[3]));

            int[] clear = new int[4];
            glGetIntegerv(GL_COLOR_CLEAR_VALUE, clear);
            LOGGER.info("GL_COLOR_CLEAR_VALUE: " + String.format("x:%d, y:%d, w:%d, h:%d", clear[0], clear[1], clear[2], clear[3]));

            LOGGER.info("GL_BLEND_SRC_RGB: " + glGetInteger(GL_BLEND_SRC_RGB));
            LOGGER.info("GL_BLEND_SRC_ALPHA: " + glGetInteger(GL_BLEND_SRC_ALPHA));
            LOGGER.info("GL_BLEND_DST_RGB: " + glGetInteger(GL_BLEND_DST_RGB));
            LOGGER.info("GL_BLEND_DST_ALPHA: " + glGetInteger(GL_BLEND_DST_ALPHA));
*/

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
}
