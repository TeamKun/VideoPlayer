package net.kunmc.lab.vplayer.video;

import net.kunmc.lab.vplayer.model.PlayState;

import java.util.UUID;

public class VDisplay extends VDisplayAbstract {

    protected final VPlayStateStore playStateStore = new VPlayStateStore();

    public VDisplay(UUID uuidIn) {
        super(uuidIn);
    }

    @Override
    public PlayState fetchState() {
        return playStateStore.fetch();
    }

    @Override
    public void dispatchState(PlayState action) {
        playStateStore.dispatch(action);
    }

}
