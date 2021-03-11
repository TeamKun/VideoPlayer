package net.kunmc.lab.vplayer.client.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.common.model.PlayState;
import net.kunmc.lab.vplayer.common.video.VDisplayAbstract;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nonnull;
import java.util.UUID;

public class VDisplayClient extends VDisplayAbstract {

    protected final VPlayStateStoreClient playStateStore = new VPlayStateStoreClient();
    private VRequestedState requestedState = VRequestedState.VALIDATE;
    private VPlayerClient client;
    private PlayState playStateQueue;

    public VDisplayClient(UUID uuidIn) {
        super(uuidIn);
    }

    @Nonnull
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
        if (quad == null)
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

    public VControllerClient getController() {
        return client.getController();
    }

    public boolean processRequest() {
        switch (requestedState) {
            case VALIDATE:
                client = new VPlayerClient();
                client.init();
                state = VState.VALIDATED;
                requestedState = VRequestedState.NONE;
                playStateStore.reapply(this);
                playStateStore.observe(this);
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

    private enum VRequestedState {
        NONE,
        VALIDATE,
        INVALIDATE,
    }
}
