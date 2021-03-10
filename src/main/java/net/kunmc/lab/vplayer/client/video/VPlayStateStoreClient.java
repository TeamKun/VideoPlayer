package net.kunmc.lab.vplayer.client.video;

import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.patch.VideoPatch;
import net.kunmc.lab.vplayer.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.video.VController;
import net.kunmc.lab.vplayer.video.VPlayStateStore;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class VPlayStateStoreClient extends VPlayStateStore {
    public void reapply(VDisplayClient display) {
        VController controller = display.getController();
        controller.setFile(file).thenRun(() -> {
            controller.setTime(timer.getTime());
            controller.setPaused(paused);
        });
    }

    public void dispatch(VDisplayClient display, PlayState action) {
        VController controller = display.getController();
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
                        Collections.singletonList(new VideoPatch(display.getUUID(), display.getQuad(), fetch()))));
            }
        });
        display.getController().onLoadObserve().thenAccept(p -> {
            display.getController().setTime(timer.getTime());
        });
    }
}
