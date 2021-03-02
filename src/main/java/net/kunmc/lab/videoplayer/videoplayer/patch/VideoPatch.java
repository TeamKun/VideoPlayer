package net.kunmc.lab.videoplayer.videoplayer.patch;

import net.kunmc.lab.videoplayer.videoplayer.model.PlayState;
import net.kunmc.lab.videoplayer.videoplayer.model.Quad;

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
