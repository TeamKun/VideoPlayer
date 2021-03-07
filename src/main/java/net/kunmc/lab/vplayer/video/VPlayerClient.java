package net.kunmc.lab.vplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.model.Quad;
import net.kunmc.lab.vplayer.mpv.MPlayerClient;

import java.util.Optional;

public class VPlayerClient implements VEventHandler {
    private final MPlayerClient playerClient;
    private final VRenderer renderer;
    private boolean started;

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

    public Optional<String> command(String[] args) {
        return playerClient.command(args);
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
