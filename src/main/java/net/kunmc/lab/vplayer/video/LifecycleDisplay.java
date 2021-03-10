package net.kunmc.lab.vplayer.video;

import net.kunmc.lab.vplayer.model.Display;

public interface LifecycleDisplay extends Display {
    void destroy();

    boolean isDestroyed();

    public enum VState {
        INVALIDATED,
        VALIDATED,
    }
}
