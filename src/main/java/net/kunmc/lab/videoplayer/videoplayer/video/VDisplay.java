package net.kunmc.lab.videoplayer.videoplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

public class VDisplay {
    private VQuad quad;
    private VState state = VState.INVALIDATED;
    private VRequestedState requestedState = VRequestedState.VALIDATE;
    private VPlayerClient client;
    private boolean destroyRequested;
    private final Deque<String[]> commandQueue = new ArrayDeque<>();

    public void setQuad(VQuad quadIn) {
        quad = quadIn;
    }

    public VQuad getQuad() {
        return quad;
    }

    public void renderFrame() {
        if (state != VState.VALIDATED)
            return;
        client.renderFrame();
    }

    public void render(MatrixStack stack) {
        if (state != VState.VALIDATED)
            return;
        if (quad != null)
            client.render(stack, quad);
    }

    public boolean canSee() {
        if (destroyRequested)
            return false;
        ActiveRenderInfo activeRenderInfo = Minecraft.getInstance().gameRenderer.getActiveRenderInfo();
        Vec3d view = activeRenderInfo.getProjectedView();
        double distance = quad.getNearestDistance(view);
        return distance < 96 && activeRenderInfo.getRenderViewEntity().dimension.equals(quad.dimension);
    }

    public void validate() {
        if (state != VState.INVALIDATED)
            return;
        requestedState = VRequestedState.VALIDATE;
    }

    public void invalidate() {
        if (state != VState.VALIDATED)
            return;
        requestedState = VRequestedState.INVALIDATE;
    }

    public void destroy() {
        invalidate();
        destroyRequested = true;
    }

    public boolean isDestroyed() {
        return state == VState.INVALIDATED && destroyRequested;
    }

    public boolean processRequest() {
        switch (requestedState) {
            case VALIDATE:
                client = new VPlayerClient();
                client.init();
                processCommand();
                state = VState.VALIDATED;
                requestedState = VRequestedState.NONE;
                break;
            case INVALIDATE:
                client.destroy();
                client = null;
                state = VState.INVALIDATED;
                requestedState = VRequestedState.NONE;
                break;
        }
        return isDestroyed();
    }

    public void command(String... args) {
        switch (state) {
            case INVALIDATED:
                commandQueue.add(args);
                break;
            case VALIDATED:
                client.command(args);
                break;
            default:
                throw new IllegalStateException("Invalid State");
        }
    }

    private void processCommand() {
        String[] command;
        while ((command = commandQueue.poll()) != null) {
            client.command(command);
        }
    }

    private enum VState {
        INVALIDATED,
        VALIDATED,
    }

    private enum VRequestedState {
        NONE,
        VALIDATE,
        INVALIDATE,
    }
}
