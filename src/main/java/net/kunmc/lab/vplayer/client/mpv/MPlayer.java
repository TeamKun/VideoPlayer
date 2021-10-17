package net.kunmc.lab.vplayer.client.mpv;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import cz.adamh.utils.NativeUtils;
import net.kunmc.lab.vplayer.VideoPlayer;
import net.kunmc.lab.vplayer.common.util.HashUtils;
import net.minecraft.util.Util;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
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
        switch (Util.getPlatform()) {
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
        if (Files.exists(ytdlPath)) {
            String hash = HashUtils.fileToMD5(ytdlPath);
            if (!"7f492c4db1af1ee43adfce37071ef317".equalsIgnoreCase(hash)) {
                try {
                    Files.delete(ytdlPath);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "youtube-dl.exeのアップデートに失敗しました。一旦すべてのMinecraftを終了してから起動し直してください。");
                    throw new RuntimeException("youtube-dl.exeのアップデートに失敗しました。一旦すべてのMinecraftを終了してから起動し直してください。");
                }
            }
        }
        if (!Files.exists(ytdlPath)) {
            try {
                InputStream stream = MPlayer.class.getResourceAsStream("/natives/yt-dlp.exe");
                if (stream != null)
                    Files.copy(stream, ytdlPath);
            } catch (IOException e) {
                VideoPlayer.LOGGER.warn("YouTube-DL Error", e);
            }
        }
    }
}
