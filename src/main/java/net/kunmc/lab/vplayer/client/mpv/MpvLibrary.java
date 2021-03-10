package net.kunmc.lab.vplayer.client.mpv;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface MpvLibrary extends StdCallLibrary {
    MpvLibrary INSTANCE = (MpvLibrary) Native.loadLibrary("mpv", MpvLibrary.class);

    /*
     * Event ID's
     */
    interface mpv_event_id {
        int MPV_EVENT_NONE = 0;
        int MPV_EVENT_SHUTDOWN = 1;
        int MPV_EVENT_LOG_MESSAGE = 2;
        int MPV_EVENT_GET_PROPERTY_REPLY = 3;
        int MPV_EVENT_SET_PROPERTY_REPLY = 4;
        int MPV_EVENT_COMMAND_REPLY = 5;
        int MPV_EVENT_START_FILE = 6;
        int MPV_EVENT_END_FILE = 7;
        int MPV_EVENT_FILE_LOADED = 8;
        int MPV_EVENT_TRACKS_CHANGED = 9;
        int MPV_EVENT_TRACK_SWITCHED = 10;
        int MPV_EVENT_IDLE = 11;
        int MPV_EVENT_PAUSE = 12;
        int MPV_EVENT_UNPAUSE = 13;
        int MPV_EVENT_TICK = 14;
        int MPV_EVENT_SCRIPT_INPUT_DISPATCH = 15;
        int MPV_EVENT_CLIENT_MESSAGE = 16;
        int MPV_EVENT_VIDEO_RECONFIG = 17;
        int MPV_EVENT_AUDIO_RECONFIG = 18;
        int MPV_EVENT_METADATA_UPDATE = 19;
        int MPV_EVENT_SEEK = 20;
        int MPV_EVENT_PLAYBACK_RESTART = 21;
        int MPV_EVENT_PROPERTY_CHANGE = 22;
        int MPV_EVENT_CHAPTER_CHANGE = 23;
        int MPV_EVENT_QUEUE_OVERFLOW = 24;
        int MPV_EVENT_HOOK = 25;
    }

    /*
     * Render Param Type ID
     */
    interface mpv_render_param_type {
        int MPV_RENDER_PARAM_INVALID = 0;
        int MPV_RENDER_PARAM_API_TYPE = 1;
        int MPV_RENDER_PARAM_OPENGL_INIT_PARAMS = 2;
        int MPV_RENDER_PARAM_OPENGL_FBO = 3;
        int MPV_RENDER_PARAM_FLIP_Y = 4;
        int MPV_RENDER_PARAM_DEPTH = 5;
        int MPV_RENDER_PARAM_ICC_PROFILE = 6;
        int MPV_RENDER_PARAM_AMBIENT_LIGHT = 7;
        int MPV_RENDER_PARAM_X11_DISPLAY = 8;
        int MPV_RENDER_PARAM_WL_DISPLAY = 9;
        int MPV_RENDER_PARAM_ADVANCED_CONTROL = 10;
        int MPV_RENDER_PARAM_NEXT_FRAME_INFO = 11;
        int MPV_RENDER_PARAM_BLOCK_FOR_TARGET_TIME = 12;
        int MPV_RENDER_PARAM_SKIP_RENDERING = 13;
        int MPV_RENDER_PARAM_DRM_DISPLAY = 14;
        int MPV_RENDER_PARAM_DRM_DRAW_SURFACE_SIZE = 15;
        int MPV_RENDER_PARAM_DRM_DISPLAY_V2 = 16;
        int MPV_RENDER_PARAM_SW_SIZE = 17;
        int MPV_RENDER_PARAM_SW_FORMAT = 18;
        int MPV_RENDER_PARAM_SW_STRIDE = 19;
        int MPV_RENDER_PARAM_SW_POINTER = 20;
    }

    interface mpv_render_update_flag {
        int MPV_RENDER_UPDATE_FRAME = 1;
    }

    interface mpv_format {
        int MPV_FORMAT_NONE = 0;
        int MPV_FORMAT_STRING = 1;
        int MPV_FORMAT_OSD_STRING = 2;
        int MPV_FORMAT_FLAG = 3;
        int MPV_FORMAT_INT64 = 4;
        int MPV_FORMAT_DOUBLE = 5;
        int MPV_FORMAT_NODE = 6;
        int MPV_FORMAT_NODE_ARRAY = 7;
        int MPV_FORMAT_NODE_MAP = 8;
        int MPV_FORMAT_BYTE_ARRAY = 9;
    }

    long mpv_client_api_version();

    long mpv_create();

    long mpv_create_client(long ctx, String name);

    int mpv_initialize(long handle);

    void mpv_destroy(long ctx);

    int mpv_command(long handle, String[] args);

    int mpv_command_async(long handle, long reply_userdata, String[] args);

    int mpv_command_string(long handle, String args);

    Pointer mpv_get_property_string(long handle, String name);

    int mpv_set_property_string(long handle, String name, String data);

    int mpv_set_property_async(long handle, long reply_userdata, String name, int format, Pointer data);

    int mpv_get_property_async(long handle, long reply_userdata, String name, int format);

    int mpv_observe_property(long handle, long reply_userdata, String name, int format);

    int mpv_unobserve_property(long handle, long registered_reply_userdata);

    void mpv_free(Pointer data);

    int mpv_set_option_string(long handle, String name, String data);

    int mpv_set_option(long handle, String name, int format, Pointer data);

    mpv_event mpv_wait_event(long handle, double timeOut);

    int mpv_request_event(long handle, int event_id, int enable);

    String mpv_event_name(int event);

    String mpv_error_string(int error);

    void mpv_terminate_destroy(long handle);

    int mpv_render_context_create(PointerByReference render_context, long handle, mpv_render_param params);

    void mpv_render_context_free(Pointer render_context);

    void mpv_set_wakeup_callback(long handle, on_wakeup callback, Pointer d);

    void mpv_render_context_set_update_callback(Pointer render_context, on_render_update callback, Pointer d);

    int mpv_render_context_update(Pointer render_context);

    int mpv_render_context_render(Pointer render_context, mpv_render_param params);

    void mpv_render_context_report_swap(Pointer render_context);

    class mpv_event extends Structure {
        public int event_id;
        public int error;
        public long reply_userdata;
        public Pointer data;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("event_id", "error", "reply_userdata", "data");
        }
    }

    class mpv_event_property extends Structure {
        public String name;
        public int format;
        public Pointer data;

        public mpv_event_property() {
        }

        public mpv_event_property(Pointer pointer) {
            super(pointer);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("name", "format", "data");
        }
    }

    class mpv_node extends Structure {
        u u;
        mpv_format format;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("u", "format");
        }

        public static class u extends Union {
            String string;
            int flag;
            long int64;
            double double_;
            Pointer list;
            Pointer ba;
        }
    }

    class mpv_node_list extends Structure {
        int num;
        mpv_node[] values;
        @Deprecated
        Pointer keys;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("num", "values", "keys");
        }
    }

    class mpv_byte_array extends Structure {
        Pointer data;
        long size;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "size");
        }
    }

    class mpv_event_command extends Structure {
        public mpv_event_command() {
        }

        public mpv_event_command(Pointer pointer) {
            super(pointer);
        }

        mpv_node result;

        @Override
        protected List<String> getFieldOrder() {
            return Collections.singletonList("result");
        }
    }

    class mpv_opengl_init_params extends Structure {
        public get_proc_address get_proc_address;
        public Pointer get_proc_address_ctx;
        public String extra_exts;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("get_proc_address", "get_proc_address_ctx", "extra_exts");
        }
    }

    class mpv_render_param extends Structure {
        public int type;
        public Pointer data;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("type", "data");
        }
    }

    class mpv_opengl_fbo extends Structure {
        public int fbo;
        public int w, h;
        public int internal_format;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("fbo", "w", "h", "internal_format");
        }
    }

    interface get_proc_address extends StdCallCallback {
        Pointer callback(Pointer handle, String name);
    }

    interface on_wakeup extends StdCallCallback {
        void callback(Pointer d);
    }

    interface on_render_update extends StdCallCallback {
        void callback(Pointer d);
    }
}