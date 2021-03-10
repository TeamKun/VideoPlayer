package net.kunmc.lab.vplayer.common.model;

import javax.annotation.Nullable;
import java.util.UUID;

public interface Display {
    UUID getUUID();

    void setQuad(@Nullable Quad quadIn);
    
    @Nullable
    Quad getQuad();

    PlayState fetchState();

    void dispatchState(PlayState action);
}
