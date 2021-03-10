package net.kunmc.lab.vplayer.common.video;

import net.kunmc.lab.vplayer.common.model.LifecycleDisplay;
import net.kunmc.lab.vplayer.common.model.Quad;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class VDisplayAbstract implements LifecycleDisplay {
    protected final UUID uuid;
    @Nullable
    protected Quad quad;
    protected VState state = VState.INVALIDATED;
    protected boolean destroyRequested;

    public VDisplayAbstract(UUID uuidIn) {
        uuid = uuidIn;
    }

    @Override
    public UUID getUUID() {
        return uuid;
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
    public void destroy() {
        destroyRequested = true;
        state = VState.INVALIDATED;
    }

    @Override
    public boolean isDestroyed() {
        return state == VState.INVALIDATED && destroyRequested;
    }
}
