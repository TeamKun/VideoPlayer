package net.kunmc.lab.vplayer.common.patch;

import net.minecraftforge.eventbus.api.Event;

import java.util.List;

public abstract class VideoPatchEvent extends Event {
    private final VideoPatchOperation operation;
    private final List<VideoPatch> patches;

    public VideoPatchOperation getOperation() {
        return operation;
    }

    public List<VideoPatch> getPatches() {
        return patches;
    }

    public VideoPatchEvent(VideoPatchOperation operation, List<VideoPatch> patches) {
        this.operation = operation;
        this.patches = patches;
    }
}
