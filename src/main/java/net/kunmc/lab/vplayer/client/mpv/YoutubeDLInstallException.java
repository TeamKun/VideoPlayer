package net.kunmc.lab.vplayer.client.mpv;

public class YoutubeDLInstallException extends RuntimeException {
    public YoutubeDLInstallException(String reason) {
        super(reason);
    }

    public YoutubeDLInstallException(String message, Throwable cause) {
        super(message, cause);
    }
}
