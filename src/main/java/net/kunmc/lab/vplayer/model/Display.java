package net.kunmc.lab.vplayer.model;

public interface Display {
    void setQuad(Quad quadIn);

    Quad getQuad();

    PlayState fetchState();

    void dispatchState(PlayState action);
}
