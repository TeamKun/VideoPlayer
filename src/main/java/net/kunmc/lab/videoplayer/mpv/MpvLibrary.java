package net.kunmc.lab.videoplayer.mpv;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface MpvLibrary extends Library {
    MpvLibrary INSTANCE = (MpvLibrary) Native.loadLibrary("mpv", MpvLibrary.class);

    Pointer mpv_create();

    int mpv_set_option_string(Pointer ctx, String name, String data);

    String mpv_error_string(int error);

    int mpv_set_option(Pointer ctx, String name, int format, Pointer data);

    int mpv_initialize(Pointer ctx);

    default void check_error(int status) throws RuntimeException {
        if (status < 0) {
            throw new RuntimeException("mpv API error: " + mpv_error_string(status));
        }
    }
}