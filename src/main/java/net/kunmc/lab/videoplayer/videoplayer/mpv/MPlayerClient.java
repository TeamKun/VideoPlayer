package net.kunmc.lab.videoplayer.videoplayer.mpv;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.PointerByReference;
import net.kunmc.lab.videoplayer.videoplayer.video.VEventHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Optional;

import static net.kunmc.lab.videoplayer.videoplayer.mpv.MPlayer.get_proc_address;
import static net.kunmc.lab.videoplayer.videoplayer.mpv.MPlayer.*;
import static net.kunmc.lab.videoplayer.videoplayer.mpv.MpvLibrary.*;
import static org.lwjgl.opengl.GL11.GL_RGB8;

public class MPlayerClient {
    private long handle;
    private mpv_opengl_fbo fbo_settings;
    private mpv_render_param head_render_param;
    private PointerByReference mpv_gl;
    private final DoubleByReference volumeRef = new DoubleByReference();

    private long dwidth;
    private long dheight;

    private boolean redraw = false;
    public final on_render_update on_mpv_redraw = d -> {
        redraw = true;
    };

    public void init() {
        handle = mpv.mpv_create();
        if (handle == 0)
            throw new RuntimeException("failed creating context");

        // mpv.mpv_set_option_string(handle, "terminal", "yes");
        mpv.mpv_set_option_string(handle, "msg-level", "all=v");

        validateStatus(mpv, mpv.mpv_initialize(handle));

        initMpvRenderer(mpv, handle);
    }

    public Optional<String> command(String[] args) {
        // Play this file.
        return getStatus(mpv, mpv.mpv_command_async(handle, 0, ArrayUtils.add(args, null)));
    }

    public void render() {
        renderMpv();

        mpv.mpv_render_context_report_swap(mpv_gl.getValue());
    }

    public void setVolume(double volume) {
        volumeRef.setValue(Math.max(0, Math.min(1, volume)) * 100);
    }

    public void destroy() {
        mpv.mpv_render_context_free(mpv_gl.getValue());
        mpv_gl.setValue(null);
        mpv.mpv_terminate_destroy(handle);
    }

    private void renderMpv() {
        if (redraw) {
            redraw = false;

            int flags = mpv.mpv_render_context_update(mpv_gl.getValue());
            if ((flags & MpvLibrary.MPV_RENDER_UPDATE_FRAME) != 0) {
                mpv.mpv_render_context_render(mpv_gl.getValue(), head_render_param);
                mpv.mpv_set_property_async(handle, 0, "volume", MpvLibrary.MPV_FORMAT_DOUBLE, volumeRef.getPointer());
            }
        }
    }

    public void processEvent(VEventHandler handler) {
        mpv_event event = mpv.mpv_wait_event(handle, 0);
        switch (event.event_id) {
            case MPV_EVENT_VIDEO_RECONFIG: {
                mpv.mpv_get_property_async(handle, 0, "dwidth", MPV_FORMAT_INT64);
                mpv.mpv_get_property_async(handle, 1, "dheight", MPV_FORMAT_INT64);
            }
            break;

            case MPV_EVENT_GET_PROPERTY_REPLY: {
                mpv_event_property prop = new mpv_event_property(event.data);
                prop.read();
                long data = prop.data.getLong(0);
                switch ((int) event.reply_userdata) {
                    case 0:
                        dwidth = data;
                        break;
                    case 1:
                        dheight = data;
                        break;
                }

                if (dwidth > 0 && dheight > 0)
                    handler.onResize((int) dwidth, (int) dheight);
            }
            break;

            default:
                break;
        }
    }

    public void initFbo(int fbo) {
        fbo_settings.fbo = fbo;
        fbo_settings.write();
    }

    public void updateFbo(int _width, int _height) {
        fbo_settings.w = _width;
        fbo_settings.h = _height;
        fbo_settings.write();
    }

    private void initMpvRenderer(MpvLibrary mpv, long handle) {
        mpv_opengl_init_params gl_init_params = new mpv_opengl_init_params();
        gl_init_params.get_proc_address = get_proc_address;
        gl_init_params.get_proc_address_ctx = null;
        gl_init_params.extra_exts = null;
        gl_init_params.write();

        String MPV_RENDER_API_TYPE_OPENGL_STR = "opengl";
        Pointer MPV_RENDER_API_TYPE_OPENGL = new Memory(MPV_RENDER_API_TYPE_OPENGL_STR.getBytes().length + 1);
        MPV_RENDER_API_TYPE_OPENGL.setString(0, MPV_RENDER_API_TYPE_OPENGL_STR);

        mpv_render_param head_init_param = new mpv_render_param();
        mpv_render_param[] init_params = (mpv_render_param[]) head_init_param.toArray(4);
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
        validateStatus(mpv, mpv.mpv_render_context_create(mpv_gl, handle, head_init_param));

        mpv.mpv_render_context_set_update_callback(mpv_gl.getValue(), on_mpv_redraw, null);

        // Render Params
        fbo_settings = new mpv_opengl_fbo();
        fbo_settings.fbo = 0;
        fbo_settings.w = 0;
        fbo_settings.h = 0;
        fbo_settings.internal_format = GL_RGB8;
        fbo_settings.write();

        head_render_param = new mpv_render_param();
        mpv_render_param[] render_params = (mpv_render_param[]) head_render_param.toArray(4);
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
}
