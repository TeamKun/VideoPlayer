package net.kunmc.lab.vplayer.video;

import net.kunmc.lab.vplayer.model.Display;
import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.model.Quad;

import javax.annotation.Nullable;
import java.util.UUID;

public class VDisplay implements Display {
    protected final UUID uuid;
    protected final VPlayStateStore playStateStore = new VPlayStateStore();
    @Nullable
    protected Quad quad;
    protected VState state = VState.INVALIDATED;
    protected boolean destroyRequested;

    public VDisplay(UUID uuidIn) {
        uuid = uuidIn;
    }

    @Override
    public void setQuad(@Nullable Quad quadIn) {
        quad = quadIn;
    }

    @Nullable
    @Override
    public Quad getQuad() {
        return quad;
    }

    @Override
    public PlayState fetchState() {
        return playStateStore.fetch();
    }

    @Override
    public void dispatchState(PlayState action) {
        playStateStore.dispatch(action);
    }

    public void destroy() {
        destroyRequested = true;
        state = VState.INVALIDATED;
    }

    public boolean isDestroyed() {
        return state == VState.INVALIDATED && destroyRequested;
    }

    protected enum VState {
        INVALIDATED,
        VALIDATED,
    }

    protected enum VRequestedState {
        NONE,
        VALIDATE,
        INVALIDATE,
    }
}
