package net.kunmc.lab.vplayer.patch;

import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.model.Quad;

import java.util.UUID;

public class VideoPatch {
    private final UUID id;
    private final Quad quad;
    private final PlayState state;

    public VideoPatch(UUID id, Quad quad, PlayState state) {
        this.id = id;
        this.quad = quad;
        this.state = state;
    }

    public UUID getId() {
        return id;
    }

    public Quad getQuad() {
        return quad;
    }

    public PlayState getState() {
        return state;
    }
}
