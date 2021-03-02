package net.kunmc.lab.vplayer.video;

import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.util.Timer;

import java.util.Objects;

public class VPlayStateStore {
    private String file;
    private final Timer timer = Timer.createUnstarted();
    private boolean paused;

    public PlayState fetch() {
        PlayState state = new PlayState();
        state.file = file;
        state.time = timer.getTime();
        state.paused = paused;
        return state;
    }

    public void dispatch(VDisplay display, PlayState action) {
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
