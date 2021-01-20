package net.kunmc.lab.videoplayer;

import com.sun.jna.*;

import java.util.Arrays;
import java.util.List;

public interface MpvLibrary extends Library {
    MpvLibrary INSTANCE = (MpvLibrary) Native.loadLibrary("mpv", MpvLibrary.class);

    /*
     * Event ID's
     */
    int MPV_EVENT_END_FILE = 7;
    int MPV_EVENT_FILE_LOADED = 8;
    int MPV_EVENT_IDLE = 11;
    int MPV_EVENT_TICK = 14;

    /*
     * Render Param Type ID
     */
    int MPV_RENDER_PARAM_INVALID = 0;
    int MPV_RENDER_PARAM_API_TYPE = 1;
    int MPV_RENDER_PARAM_OPENGL_INIT_PARAMS = 2;

    long mpv_client_api_version();

    long mpv_create();

    int mpv_initialize(long handle);

    int mpv_command(long handle, String[] args);

    int mpv_command_string(long handle, String args);

    Pointer mpv_get_property_string(long handle, String name);

    int mpv_set_property_string(long handle, String name, String data);

    int mpv_set_option_string(long handle, String name, String data);

    void mpv_free(Pointer data);

    int mpv_set_option(long handle, String name, int format, Pointer data);

    mpv_event mpv_wait_event(long handle, double timeOut);

    int mpv_request_event(long handle, int event_id, int enable);

    String mpv_event_name(int event);

    String mpv_error_string(int error);

    void mpv_terminate_destroy(long handle);

    int mpv_render_context_create(Pointer res, long handle, mpv_render_param[] params);

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

        public static mpv_render_param create(int type, Pointer data) {
            mpv_render_param param = new mpv_render_param();
            param.type = type;
            param.data = data;
            return param;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("type", "data");
        }
    }

    interface get_proc_address extends Callback {
        void callback(long handle, String name);
    }
}