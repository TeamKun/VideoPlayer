package net.kunmc.lab.videoplayer.videoplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.videoplayer.videoplayer.mpv.MPlayerClient;

import java.util.Optional;

public class VPlayerClient implements VEventHandler {
    private final MPlayerClient playerClient;
    private final VRenderer renderer;

    public VPlayerClient() {
        playerClient = new MPlayerClient();
        renderer = new VRenderer();
    }

    public void init() {
        playerClient.init();
        renderer.initFbo(480, 480);
        playerClient.initFbo(renderer.getFramebuffer().framebufferObject);
        playerClient.updateFbo(480, 480);
    }

    public Optional<String> command(String[] args) {
        return playerClient.command(args);
    }

    public void renderFrame() {
        playerClient.render();
    }

    public void render(MatrixStack stack, VQuad quad) {
        playerClient.setVolume(renderer.getVolume(quad));
        playerClient.processEvent(this);
        renderer.render(stack, quad);
    }

    @Override
    public void onResize(int width, int height) {
        playerClient.updateFbo(width, height);
        renderer.updateFbo(width, height);
    }

    public void destroy() {
        playerClient.destroy();
        renderer.destroy();
    }
}
