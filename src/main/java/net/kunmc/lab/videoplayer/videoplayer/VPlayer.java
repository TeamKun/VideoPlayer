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
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.util.Arrays;

import static net.kunmc.lab.videoplayer.videoplayer.MpvLibrary.*;
import static net.minecraft.client.Minecraft.IS_RUNNING_ON_MAC;
import static org.lwjgl.glfw.GLFW.glfwGetProcAddress;
import static org.lwjgl.opengl.GL11.*;

public class VPlayer {
    private MpvLibrary mpv;
    private static final IntByReference zero = new IntByReference(0);
    private static final IntByReference one = new IntByReference(1);

    public static void check_error(MpvLibrary mpv, int status) throws RuntimeException {
        if (status < 0)
            throw new RuntimeException("mpv API error: " + mpv.mpv_error_string(status));
    }

    public static final MpvLibrary.get_proc_address get_proc_address = (ctx, name) -> {
        long addr = glfwGetProcAddress(name);
        return Pointer.createConstant(addr);
    };

    public void init() {
        try {
            NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));
        } catch (IOException e) {
            throw new RuntimeException("JNA Error", e);
        }

        mpv = MpvLibrary.INSTANCE;
    }

    public void destroy() {
    }

    public class VPlayerClient {
        private long handle;
        private MpvLibrary.mpv_render_param head_render_param;
        private PointerByReference mpv_gl;
        private Framebuffer framebuffer;
        private int fbo;
        private final DoubleByReference volumeRef = new DoubleByReference();

        private boolean redraw = false;

        public final MpvLibrary.on_render_update on_mpv_redraw = d -> {
            redraw = true;
        };

        public void init() {
            handle = mpv.mpv_create();
            if (handle == 0)
                throw new RuntimeException("failed creating context");

            // mpv.mpv_set_option_string(handle, "terminal", "yes");
            mpv.mpv_set_option_string(handle, "msg-level", "all=v");

            check_error(mpv, mpv.mpv_initialize(handle));

            initMpvRenderer(mpv, handle);

            int _width = 480;
            int _height = 480;

            fbo = initFbo(_width, _height);

            initMpvFbo(_width, _height, fbo);

            // Play this file.
            check_error(mpv, mpv.mpv_command_async(handle, 0, new String[]{"loadfile", "test.mp4", null}));
        }

        public void render(MatrixStack stack, VQuad quad) {
            Vec3d view = Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView();
            Vec3d pos = Arrays.stream(quad.vertices).reduce((a, b) -> a.add(b).scale(.5)).orElse(Vec3d.ZERO);

            GameSettings gameSettings = Minecraft.getInstance().gameSettings;
            double distance = Math.min(1, Math.max(0, view.distanceTo(pos) / 48.0));
            double distance_vol = Math.pow(1 - distance, 4);
            double volume = gameSettings.getSoundLevel(SoundCategory.MASTER) * gameSettings.getSoundLevel(SoundCategory.VOICE) * distance_vol;
            volumeRef.setValue(Math.max(0, Math.min(1, volume)) * 100);

            renderMpv();

            framebuffer.bindFramebufferTexture();
            RenderSystem.disableCull();

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);

            stack.push();
            stack.translate(-view.x, -view.y, -view.z);
            Matrix4f matrix = stack.getLast().getMatrix();
            bufferbuilder.pos(matrix, (float) quad.vertices[0].x, (float) quad.vertices[0].y, (float) quad.vertices[0].z).tex(0, 1).endVertex();
            bufferbuilder.pos(matrix, (float) quad.vertices[1].x, (float) quad.vertices[1].y, (float) quad.vertices[1].z).tex(1, 1).endVertex();
            bufferbuilder.pos(matrix, (float) quad.vertices[2].x, (float) quad.vertices[2].y, (float) quad.vertices[2].z).tex(1, 0).endVertex();
            bufferbuilder.pos(matrix, (float) quad.vertices[3].x, (float) quad.vertices[3].y, (float) quad.vertices[3].z).tex(0, 0).endVertex();
            bufferbuilder.finishDrawing();
            WorldVertexBufferUploader.draw(bufferbuilder);
            stack.pop();

            RenderSystem.enableCull();
            framebuffer.unbindFramebufferTexture();

            mpv.mpv_render_context_report_swap(mpv_gl.getValue());
        }

        public void destroy() {
            mpv.mpv_render_context_free(mpv_gl.getValue());
            mpv_gl.setValue(null);
            mpv.mpv_terminate_destroy(handle);
            framebuffer.deleteFramebuffer();
        }

        private void renderMpv() {
            if (redraw) {
                redraw = false;

                int flags = mpv.mpv_render_context_update(mpv_gl.getValue());
                if ((flags & MpvLibrary.MPV_RENDER_UPDATE_FRAME) != 0) {
                    {
                        RenderSystem.pushLightingAttributes();
                        RenderSystem.pushTextureAttributes();
                        RenderSystem.pushMatrix();

                        mpv.mpv_render_context_render(mpv_gl.getValue(), head_render_param);

                        RenderSystem.popMatrix();
                        RenderSystem.popAttributes();
                        RenderSystem.popAttributes();
                    }

                    glClearColor(0, 0, 0, 0);

                    mpv.mpv_set_property_async(handle, 0, "volume", MpvLibrary.MPV_FORMAT_DOUBLE, volumeRef.getPointer());
                    Minecraft.getInstance().getFramebuffer().bindFramebuffer(true);
                }
            }

            mpv_event event = mpv.mpv_wait_event(handle, 0);
            switch (event.event_id)
            {
                case MPV_EVENT_FILE_LOADED:
                {
                    mpv.mpv_get_property_async(handle, 0, "width", MPV_FORMAT_INT64);
                    mpv.mpv_get_property_async(handle, 1, "height", MPV_FORMAT_INT64);
                }
                break;

                case MPV_EVENT_VIDEO_RECONFIG:
                {
                    mpv.mpv_get_property_async(handle, 2, "dwidth", MPV_FORMAT_INT64);
                    mpv.mpv_get_property_async(handle, 3, "dheight", MPV_FORMAT_INT64);
                }
                break;

                case MPV_EVENT_GET_PROPERTY_REPLY:
                {
                    mpv_event_property prop = new mpv_event_property(event.data);
                    prop.read();
                    long data = prop.data.getLong(0);
                    switch ((int) event.reply_userdata)
                    {
                        case 0: VideoPlayer.LOGGER.info("width: " + data); break;
                        case 1: VideoPlayer.LOGGER.info("height: " + data); break;
                        case 2: VideoPlayer.LOGGER.info("dwidth: " + data); break;
                        case 3: VideoPlayer.LOGGER.info("dheight: " + data); break;
                    }
                }
                break;

                default:
                    break;
            }
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
}
