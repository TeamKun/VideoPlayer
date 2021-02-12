package net.kunmc.lab.videoplayer.videoplayer;

import com.mojang.blaze3d.matrix.MatrixStack;
import org.apache.commons.lang3.Validate;

public class VDisplay {
    private final VQuad quad;
    private VState state = VState.CREATED;
    private VPlayer.VPlayerClient client;
    private boolean destroyRequested;

    public VDisplay(VQuad quad) {
        this.quad = quad;
    }

    public VState getState() {
        return state;
    }

    public void init(VPlayer player) {
        Validate.validState(state == VState.CREATED, "Invalid State");
        client = player.new VPlayerClient();
        client.init();
        state = VState.INITIALIZED;
    }

    public void render(MatrixStack stack) {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        client.render(stack, quad);
    }

    public void destroy() {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        destroyRequested = true;
    }

    public boolean proceedDestroy() {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        if (destroyRequested) {
            client.destroy();
            client = null;
            state = VState.DESTROYED;
            return true;
        }
        return false;
    }

    public enum VState {
        CREATED,
        INITIALIZED,
        DESTROYED,
    }
}
