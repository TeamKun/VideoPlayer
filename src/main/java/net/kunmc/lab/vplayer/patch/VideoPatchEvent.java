package net.kunmc.lab.vplayer.patch;

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

    public static class Client extends VideoPatchEvent {
        public Client(VideoPatchOperation operation, List<VideoPatch> patches) {
            super(operation, patches);
        }
    }

    public static class Server extends VideoPatchEvent {
        public Server(VideoPatchOperation operation, List<VideoPatch> patches) {
            super(operation, patches);
        }
    }
}
