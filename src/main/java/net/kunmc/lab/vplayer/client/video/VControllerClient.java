package net.kunmc.lab.vplayer.client.video;

import net.kunmc.lab.vplayer.common.util.RepeatObservable;

import java.util.concurrent.CompletableFuture;

public interface VControllerClient {
    CompletableFuture<Void> setFile(String file);

    CompletableFuture<Void> setTime(double time);

    CompletableFuture<Void> setPaused(boolean paused);

    CompletableFuture<Double> getTime();

    CompletableFuture<Boolean> isPause();

    RepeatObservable<Double> getDurationObserve();

    RepeatObservable<Boolean> isPauseObserve();

    RepeatObservable<Void> onLoadObserve();
}
