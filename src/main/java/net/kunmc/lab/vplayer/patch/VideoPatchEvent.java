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

    public interface Server {
        class ReceiveFromClient extends VideoPatchEvent {
            public ReceiveFromClient(VideoPatchOperation operation, List<VideoPatch> patches) {
                super(operation, patches);
            }
        }

        class SendToClient extends VideoPatchEvent {
            public SendToClient(VideoPatchOperation operation, List<VideoPatch> patches) {
                super(operation, patches);
            }
        }
    }

    public interface Client {
        class ReceiveFromServer extends VideoPatchEvent {
            public ReceiveFromServer(VideoPatchOperation operation, List<VideoPatch> patches) {
                super(operation, patches);
            }
        }

        class SendToServer extends VideoPatchEvent {
            public SendToServer(VideoPatchOperation operation, List<VideoPatch> patches) {
                super(operation, patches);
            }
        }
    }
}
