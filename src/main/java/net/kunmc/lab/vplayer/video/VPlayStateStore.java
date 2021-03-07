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

    public void dispatch(PlayState action) {
        file = action.file;
        timer.set(action.time);
        paused = action.paused;
        timer.setPaused(paused);
    }

    public void reapply(VDisplayClient display) {
        VDisplayController controller = display.getController();
        controller.setFile(file).thenRun(() -> {
            controller.setTime(timer.getTime());
            controller.setPaused(paused);
        });
    }

    public void dispatch(VDisplayClient display, PlayState action) {
        VDisplayController controller = display.getController();
        if (!Objects.equals(file, action.file)) {
            file = action.file;
            controller.setFile(action.file);
        }
        {
            timer.set(action.time);
            controller.setTime(timer.getTime());
        }
        {
            paused = action.paused;
            timer.setPaused(paused);
            controller.setPaused(paused);
        }
    }
}
