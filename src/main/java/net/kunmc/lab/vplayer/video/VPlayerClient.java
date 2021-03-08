package net.kunmc.lab.vplayer.video;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.model.Quad;
import net.kunmc.lab.vplayer.mpv.MPlayerClient;
import net.kunmc.lab.vplayer.util.RepeatObservable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class VPlayerClient implements VEventHandler {
    private final MPlayerClient playerClient;
    private final VRenderer renderer;
    private boolean started;

    private VDisplayController controller = new VDisplayController() {
        private final Supplier<RepeatObservable<Double>> duration = Suppliers.memoize(
                () -> VPlayerClient.this.playerClient.getDispatchers().dispatcherPropertyChange.observeAsyncDouble("duration"));
        private final Supplier<RepeatObservable<Boolean>> pause = Suppliers.memoize(
                () -> VPlayerClient.this.playerClient.getDispatchers().dispatcherPropertyChange.observeAsyncBoolean("pause"));

        @Override
        public CompletableFuture<Void> setFile(String file) {
            return playerClient.getDispatchers().dispatcherCommand.commandAsync("loadfile", file);
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
    };

    public VPlayerClient() {
        playerClient = new MPlayerClient();
        renderer = new VRenderer();
    }

    public void init() {
        playerClient.init();
        renderer.initFbo(1920, 1080);
        renderer.initFrame();
        playerClient.initFbo(renderer.getFramebuffer().framebufferObject);
        playerClient.updateFbo(1920, 1080);
    }

    public VDisplayController getController() {
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
