package net.kunmc.lab.vplayer.model;

import javax.annotation.Nullable;

public interface Display {
    void setQuad(@Nullable Quad quadIn);
    
    @Nullable
    Quad getQuad();

    PlayState fetchState();

    void dispatchState(PlayState action);
}
