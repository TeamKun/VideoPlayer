package net.kunmc.lab.vplayer.common.model;

public interface LifecycleDisplay extends Display {
    void destroy();

    boolean isDestroyed();

    enum VState {
        INVALIDATED,
        VALIDATED,
    }
}
