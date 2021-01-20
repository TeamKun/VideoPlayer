package net.kunmc.lab.videoplayer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

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
}