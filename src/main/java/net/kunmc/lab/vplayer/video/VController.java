package net.kunmc.lab.vplayer.video;

import net.kunmc.lab.vplayer.util.RepeatObservable;

import java.util.concurrent.CompletableFuture;

public interface VController {
    CompletableFuture<Void> setFile(String file);

    CompletableFuture<Void> setTime(double time);

    CompletableFuture<Void> setPaused(boolean paused);

    CompletableFuture<Double> getTime();

    CompletableFuture<Boolean> isPause();

    RepeatObservable<Double> getDurationObserve();

    RepeatObservable<Boolean> isPauseObserve();

    RepeatObservable<Void> onLoadObserve();
}
