package net.kunmc.lab.vplayer.client.video;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.client.mpv.MPlayerInstance;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.kunmc.lab.vplayer.common.util.RepeatObservable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class VPlayerClient implements VEventHandlerClient {
    private static final int LOADING_WIDTH = 1920;
    private static final int LOADING_HEIGHT = 1080;

    private final MPlayerInstance playerClient;
    private final VRendererClient renderer;
    private boolean started;

    private final RepeatObservable<Void> onLoaded = new RepeatObservable<>();

    private VControllerClient controller = new VControllerClient() {
        private final Supplier<RepeatObservable<Double>> duration = Suppliers.memoize(
                () -> VPlayerClient.this.playerClient.getDispatchers().dispatcherPropertyChange.observeAsyncDouble("duration"));
        private final Supplier<RepeatObservable<Boolean>> pause = Suppliers.memoize(
                () -> VPlayerClient.this.playerClient.getDispatchers().dispatcherPropertyChange.observeAsyncBoolean("pause"));

        @Override
        public CompletableFuture<Void> setFile(String file) {
            return playerClient.getDispatchers().dispatcherCommand.commandAsync("loadfile", Strings.nullToEmpty(file))
                    .thenRun(() -> {
                        if (file == null) {
                            playerClient.updateFbo(LOADING_WIDTH, LOADING_HEIGHT);
                            renderer.updateFbo(LOADING_WIDTH, LOADING_HEIGHT);
                            getTime().thenRun(() -> started = false);
                        }
                    });
        }

        @Override
        public CompletableFuture<Void> setTime(double time) {
            return playerClient.getDispatchers().dispatcherPropertySet.setPropertyAsyncDouble("time-pos", time);
        }

        @Override
        public CompletableFuture<Void> setPaused(boolean paused) {
            return playerClient.getDispatchers().dispatcherPropertySet.setPropertyAsyncBoolean("pause", paused);
        }

        @Override
        public CompletableFuture<Double> getTime() {
            return playerClient.getDispatchers().dispatcherPropertyGet.getPropertyAsyncDouble("time-pos");
        }

        @Override
        public CompletableFuture<Boolean> isPause() {
            return playerClient.getDispatchers().dispatcherPropertyGet.getPropertyAsyncBoolean("pause");
        }

        @Override
        public RepeatObservable<Double> getDurationObserve() {
            return duration.get();
        }

        @Override
        public RepeatObservable<Boolean> isPauseObserve() {
            return pause.get();
        }

        @Override
        public RepeatObservable<Void> onLoadObserve() {
            return onLoaded;
        }
    };

    public VPlayerClient() {
        playerClient = new MPlayerInstance();
        renderer = new VRendererClient();
    }

    public void init() {
        playerClient.init();
        renderer.initFbo(LOADING_WIDTH, LOADING_HEIGHT);
        renderer.initFrame();
        playerClient.initFbo(renderer.getFramebuffer().frameBufferId);
        playerClient.updateFbo(LOADING_WIDTH, LOADING_HEIGHT);
    }

    public VControllerClient getController() {
        return controller;
    }

    public void renderFrame() {
        playerClient.processEvent(this);
        if (!started)
            renderer.initFrame();
        playerClient.renderFrame(this);
    }

    public void render(MatrixStack stack, Quad quad) {
        playerClient.setVolume(renderer.getVolume(quad));
        renderer.render(stack, quad);
    }

    @Override
    public void onBeforeRender() {
        started = true;
    }

    @Override
    public void onLoaded() {
        onLoaded.fire(null);
    }

    @Override
    public void onResize(int width, int height) {
        playerClient.updateFbo(width, height);
        renderer.updateFbo(width, height);
        playerClient.renderImmediately();
    }

    public void destroy() {
        playerClient.destroy();
        renderer.destroy();
    }
}
