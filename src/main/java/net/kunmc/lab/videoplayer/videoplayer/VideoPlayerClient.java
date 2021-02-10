package net.kunmc.lab.videoplayer.videoplayer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import cz.adamh.utils.NativeUtils;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;

import static net.minecraft.client.Minecraft.IS_RUNNING_ON_MAC;
import static org.lwjgl.glfw.GLFW.glfwGetProcAddress;
import static org.lwjgl.opengl.GL11.*;

public class VideoPlayerClient {
    private MpvLibrary.mpv_render_param head_render_param;
    private PointerByReference mpv_gl;
    private MpvLibrary mpv;
    private long handle;
    private Framebuffer framebuffer;
    private int fbo;
    private IntByReference zero = new IntByReference(0);
    private IntByReference one = new IntByReference(1);
    private final DoubleByReference volumeRef = new DoubleByReference();

    public static void check_error(MpvLibrary mpv, int status) throws RuntimeException {
        if (status < 0)
            throw new RuntimeException("mpv API error: " + mpv.mpv_error_string(status));
    }

    public final MpvLibrary.get_proc_address get_proc_address = (ctx, name) -> {
        long addr = glfwGetProcAddress(name);
        return Pointer.createConstant(addr);
    };

    public final MpvLibrary.on_wakeup on_wakeup = d -> {
    };

    private boolean redraw = false;

    public final MpvLibrary.on_render_update on_mpv_redraw = d -> {
        redraw = true;
    };

    public void initPlayer() {
        try {
            NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));
        } catch (IOException e) {
            throw new RuntimeException("JNA Error", e);
        }

        mpv = MpvLibrary.INSTANCE;
        handle = mpv.mpv_create();
        if (handle == 0)
            throw new RuntimeException("failed creating context");

        mpv.mpv_set_option_string(handle, "terminal", "yes");
        mpv.mpv_set_option_string(handle, "msg-level", "all=v");

        check_error(mpv, mpv.mpv_initialize(handle));
        mpv.mpv_set_wakeup_callback(handle, on_wakeup, null);
    }

    public void onRender(MatrixStack stack) {
        RenderSystem.pushLightingAttributes();
        RenderSystem.pushTextureAttributes();
        RenderSystem.pushMatrix();

        Vec3d view = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();

        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        double distance = Math.min(1, Math.max(0, view.distanceTo(Vec3d.ZERO) / 48.0));
        double distance_vol = Math.pow(1 - distance, 4);
        double volume = gameSettings.getSoundLevel(SoundCategory.MASTER) * gameSettings.getSoundLevel(SoundCategory.VOICE) * distance_vol;
        volumeRef.setValue(Math.max(0, Math.min(1, volume)) * 100);

        stack.translate(-view.x, -view.y, -view.z); // translate
        RenderSystem.multMatrix(stack.getLast().getMatrix());

        if (redraw) {
            redraw = false;

            int flags = mpv.mpv_render_context_update(mpv_gl.getValue());
            if ((flags & MpvLibrary.MPV_RENDER_UPDATE_FRAME) != 0) {
                mpv.mpv_render_context_render(mpv_gl.getValue(), head_render_param);
                glClearColor(0, 0, 0, 0);

                mpv.mpv_set_property_async(handle, 0, "volume", MpvLibrary.MPV_FORMAT_DOUBLE, volumeRef.getPointer());
                Minecraft.getInstance().getFramebuffer().bindFramebuffer(true);
            }
        }

        framebuffer.bindFramebufferTexture();
        RenderSystem.enableTexture();
        RenderSystem.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0, 1, 0).tex(0, 1).endVertex();
        bufferbuilder.pos(1, 1, 0).tex(1, 1).endVertex();
        bufferbuilder.pos(1, 0, 0).tex(1, 0).endVertex();
        bufferbuilder.pos(0, 0, 0).tex(0, 0).endVertex();
        bufferbuilder.finishDrawing();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.draw(bufferbuilder);

        RenderSystem.popMatrix();
        RenderSystem.popAttributes();
        RenderSystem.popAttributes();

        mpv.mpv_render_context_report_swap(mpv_gl.getValue());
    }

    public void initRenderer() {
        initMpvRenderer(mpv, handle);

        int _width = 480;
        int _height = 480;

        fbo = initFbo(_width, _height);

        initMpvFbo(_width, _height, fbo);

        // Play this file.
        check_error(mpv, mpv.mpv_command_async(handle, 0, new String[]{"loadfile", "test.mp4", null}));
    }

    private int initFbo(int _width, int _height) {
        framebuffer = new Framebuffer(_width, _height, true, IS_RUNNING_ON_MAC);
        framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        return framebuffer.framebufferObject;
    }

    private void initMpvFbo(int _width, int _height, int fbo) {
        MpvLibrary.mpv_opengl_fbo fbo_settings = new MpvLibrary.mpv_opengl_fbo();
        fbo_settings.fbo = fbo;
        fbo_settings.w = _width;
        fbo_settings.h = _height;
        fbo_settings.internal_format = GL_RGB8;
        fbo_settings.write();

        head_render_param = new MpvLibrary.mpv_render_param();
        MpvLibrary.mpv_render_param[] render_params = (MpvLibrary.mpv_render_param[]) head_render_param.toArray(4);
        render_params[0].type = MpvLibrary.MPV_RENDER_PARAM_OPENGL_FBO;
        render_params[0].data = fbo_settings.getPointer();
        render_params[0].write();
        render_params[1].type = MpvLibrary.MPV_RENDER_PARAM_FLIP_Y;
        render_params[1].data = one.getPointer();
        render_params[1].write();
        render_params[2].type = MpvLibrary.MPV_RENDER_PARAM_BLOCK_FOR_TARGET_TIME;
        render_params[2].data = zero.getPointer();
        render_params[2].write();
        render_params[3].type = MpvLibrary.MPV_RENDER_PARAM_INVALID;
        render_params[3].data = null;
        render_params[3].write();
    }

    private void initMpvRenderer(MpvLibrary mpv, long handle) {
        MpvLibrary.mpv_opengl_init_params gl_init_params = new MpvLibrary.mpv_opengl_init_params();
        gl_init_params.get_proc_address = get_proc_address;
        gl_init_params.get_proc_address_ctx = null;
        gl_init_params.extra_exts = null;
        gl_init_params.write();

        String MPV_RENDER_API_TYPE_OPENGL_STR = "opengl";
        Pointer MPV_RENDER_API_TYPE_OPENGL = new Memory(MPV_RENDER_API_TYPE_OPENGL_STR.getBytes().length + 1);
        MPV_RENDER_API_TYPE_OPENGL.setString(0, MPV_RENDER_API_TYPE_OPENGL_STR);

        MpvLibrary.mpv_render_param head_init_param = new MpvLibrary.mpv_render_param();
        MpvLibrary.mpv_render_param[] init_params = (MpvLibrary.mpv_render_param[]) head_init_param.toArray(4);
        init_params[0].type = MpvLibrary.MPV_RENDER_PARAM_API_TYPE;
        init_params[0].data = MPV_RENDER_API_TYPE_OPENGL;
        init_params[0].write();
        init_params[1].type = MpvLibrary.MPV_RENDER_PARAM_OPENGL_INIT_PARAMS;
        init_params[1].data = gl_init_params.getPointer();
        init_params[1].write();
        init_params[2].type = MpvLibrary.MPV_RENDER_PARAM_ADVANCED_CONTROL;
        init_params[2].data = one.getPointer();
        init_params[2].write();
        init_params[3].type = MpvLibrary.MPV_RENDER_PARAM_INVALID;
        init_params[3].data = null;
        init_params[3].write();

        mpv_gl = new PointerByReference();
        mpv_gl.setValue(null);

        // check_error(mpv, mpv.mpv_render_context_create(mpv_gl.getPointer(), handle, param));
        check_error(mpv, mpv.mpv_render_context_create(mpv_gl, handle, head_init_param));

        mpv.mpv_render_context_set_update_callback(mpv_gl.getValue(), on_mpv_redraw, null);
    }
}
