package net.kunmc.lab.videoplayer.videoplayer;

public class VDisplay {
    private VPlayer player;
    private VQuad quad;
    private VState state = VState.CREATED;



    public enum VState {
        CREATED,
        INITIALIZED,
        PLAYING,
        DESTROYED,
    }
}
