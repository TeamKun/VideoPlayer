package net.kunmc.lab.vplayer.client.mpv;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import cz.adamh.utils.NativeUtils;
import net.kunmc.lab.vplayer.VideoPlayer;
import net.minecraft.util.Util;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        switch (Util.getOSType()) {
            case WINDOWS:
                try {
                    NativeUtils.loadLibraryFromJar("/natives/" + System.mapLibraryName("mpv"));

                    mpv = MpvLibrary.INSTANCE;
                } catch (IOException e) {
                    VideoPlayer.LOGGER.warn("JNA Error", e);
                }
                break;
        }

        Path ytdlPath = FMLPaths.GAMEDIR.get().resolve("youtube-dl.exe");
        if (!Files.exists(ytdlPath)) {
            try {
                Files.copy(MPlayer.class.getResourceAsStream("/natives/youtube-dl.exe"), ytdlPath);
            } catch (IOException e) {
                VideoPlayer.LOGGER.warn("YouTube-DL Error", e);
            }
        }
    }
}
