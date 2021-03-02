package net.kunmc.lab.vplayer.mpv;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import cz.adamh.utils.NativeUtils;

import java.io.IOException;
import java.util.Optional;

import static org.lwjgl.glfw.GLFW.glfwGetProcAddress;

public class MPlayer {
    public static MpvLibrary mpv;
    public static final IntByReference zero = new IntByReference(0);
    public static final IntByReference one = new IntByReference(1);

    public static void validateStatus(MpvLibrary mpv, int status) throws RuntimeException {
        if (status < 0)
            throw new RuntimeException("mpv API error: " + mpv.mpv_error_string(status));
    }

    public static Optional<String> getStatus(MpvLibrary mpv, int status) throws RuntimeException {
        if (status < 0)
            return Optional.of(mpv.mpv_error_string(status));
        return Optional.empty();
    }

    public static final MpvLibrary.get_proc_address get_proc_address = (ctx, name) -> {
        long addr = glfwGetProcAddress(name);
        return Pointer.createConstant(addr);
    };

    public static void init() {
        try {
            NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));
        } catch (IOException e) {
            throw new RuntimeException("JNA Error", e);
        }

        mpv = MpvLibrary.INSTANCE;
    }
}
