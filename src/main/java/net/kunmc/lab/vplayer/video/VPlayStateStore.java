package net.kunmc.lab.vplayer.video;

import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.patch.VideoPatch;
import net.kunmc.lab.vplayer.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.util.Timer;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class VPlayStateStore {
    private String file;
    private final Timer timer = Timer.createUnstarted();
    private boolean paused;
    private float duration = -1;

    public PlayState fetch() {
        PlayState state = new PlayState();
        state.file = file;
        state.time = timer.getTime();
        state.paused = paused;
        state.duration = duration;
        return state;
    }

    public void dispatch(PlayState action) {
        file = action.file;
        timer.set(action.time);
        paused = action.paused;
        timer.setPaused(paused);
        duration = action.duration;
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
        CompletableFuture<Void> fileFuture = CompletableFuture.completedFuture(null);
        if (!Objects.equals(file, action.file)) {
            file = action.file;
            fileFuture = controller.setFile(action.file);
        }
        fileFuture.thenRun(() -> {
            {
                timer.set(action.time);
                controller.setTime(timer.getTime());
            }
            {
                paused = action.paused;
                timer.setPaused(paused);
                controller.setPaused(paused);
            }
            duration = action.duration;
        });
    }

    public void observe(VDisplayClient display) {
        display.getController().getDurationObserve().thenAccept(d -> {
            if (d != null) {
                duration = (float) (double) d;

                MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Client.SendToServer(VideoPatchOperation.UPDATE,
                        Collections.singletonList(new VideoPatch(display.uuid, display.quad, fetch()))));
            }
        });
        display.getController().isPauseObserve().thenAccept(p -> {
            if (p != null)
                paused = p;
            display.getController().getTime().thenAccept(t -> {
                if (t != null)
                    timer.set((float) (double) t);
            });
        });
    }
}
