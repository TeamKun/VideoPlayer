package net.kunmc.lab.vplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.model.PlayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.Vec3d;

public class VDisplayClient extends VDisplay {

    private VRequestedState requestedState = VRequestedState.VALIDATE;
    private VPlayerClient client;
    private PlayState playStateQueue;

    @Override
    public PlayState fetchState() {
        if (playStateQueue != null)
            return playStateQueue;
        return playStateStore.fetch();
    }

    @Override
    public void dispatchState(PlayState action) {
        playStateQueue = action;
    }

    public void renderFrame() {
        if (state != VState.VALIDATED)
            return;
        client.renderFrame();
    }

    public void render(MatrixStack stack) {
        if (state != VState.VALIDATED)
            return;
        if (quad != null) {
            processPlayState();
            client.render(stack, quad);
        }
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

    public void command(String... args) {
        client.command(args);
    }

    public boolean processRequest() {
        switch (requestedState) {
            case VALIDATE:
                client = new VPlayerClient();
                client.init();
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

    private void processPlayState() {
        if (playStateQueue != null) {
            playStateStore.dispatch(this, playStateQueue);
            playStateQueue = null;
        }
    }

}
