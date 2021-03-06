package net.kunmc.lab.vplayer.video;

import net.kunmc.lab.vplayer.model.Display;
import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.model.Quad;

public class VDisplay implements Display {
    protected final VPlayStateStore playStateStore = new VPlayStateStore();
    protected Quad quad;
    protected VState state = VState.INVALIDATED;
    protected boolean destroyRequested;

    @Override
    public void setQuad(Quad quadIn) {
        quad = quadIn;
    }

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
