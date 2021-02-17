package net.kunmc.lab.videoplayer.videoplayer.video;

import net.kunmc.lab.videoplayer.videoplayer.util.Timer;

import java.util.Objects;

public class VPlayStateStore {
    private String file;
    private final Timer timer = Timer.createUnstarted();
    private boolean paused;

    public void dispatch(VDisplay display, VPlayState action) {
        if (!Objects.equals(file, action.file)) {
            file = action.file;
            display.command("loadfile", action.file);
        }
        {
            timer.set(action.time);
            display.command("seek", String.format("%.2f", timer.getTime()), "absolute");
        }
        {
            paused = action.paused;
            timer.setPaused(paused);
            display.command("set", "pause", paused ? "yes" : "no");
        }
    }
}
