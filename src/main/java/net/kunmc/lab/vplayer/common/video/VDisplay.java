package net.kunmc.lab.vplayer.common.video;

import net.kunmc.lab.vplayer.common.model.PlayState;

import javax.annotation.Nonnull;
import java.util.UUID;

public class VDisplay extends VDisplayAbstract {

    protected final VPlayStateStore playStateStore = new VPlayStateStore();

    public VDisplay(UUID uuidIn) {
        super(uuidIn);
    }

    @Nonnull
    @Override
    public PlayState fetchState() {
        return playStateStore.fetch();
    }

    @Override
    public void dispatchState(PlayState action) {
        playStateStore.dispatch(action);
    }

}
