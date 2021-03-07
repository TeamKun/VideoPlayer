package net.kunmc.lab.vplayer.video;

import java.util.concurrent.CompletableFuture;

public interface VDisplayController {
    CompletableFuture<Void> setFile(String file);

    CompletableFuture<Void> setTime(double time);

    CompletableFuture<Void> setPaused(boolean paused);

    CompletableFuture<Double> getTime();

    CompletableFuture<Boolean> getPaused();
}
