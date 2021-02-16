package net.kunmc.lab.videoplayer.videoplayer;

import com.mojang.blaze3d.matrix.MatrixStack;
import org.apache.commons.lang3.Validate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public class VDisplay {
    private final VQuad quad;
    private VState state = VState.CREATED;
    private VPlayer.VPlayerClient client;
    private boolean destroyRequested;
    private final Deque<String[]> commandQueue = new ArrayDeque<>();

    public VDisplay(VQuad quad) {
        this.quad = quad;
    }

    public VState getState() {
        return state;
    }

    public void init(Supplier<VPlayer.VPlayerClient> clientSupplier) {
        Validate.validState(state == VState.CREATED, "Invalid State");
        client = clientSupplier.get();
        client.init();
        processCommand();
        state = VState.INITIALIZED;
    }

    public void render(MatrixStack stack) {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        client.render(stack, quad);
    }

    public void destroy() {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        destroyRequested = true;
    }

    public VDisplay command(String... args) {
        switch (state) {
            case CREATED:
                commandLater(args);
                break;
            case INITIALIZED:
                client.command(args);
                break;
            default:
                throw new IllegalStateException("Invalid State");
        }
        return this;
    }

    private void commandLater(String[] args) {
        commandQueue.add(args);
    }
    
    private void processCommand() {
        String[] command;
        while ((command = commandQueue.poll()) != null) {
            client.command(command);
        }
    }
    
    public boolean processDestroy() {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        if (destroyRequested) {
            client.destroy();
            client = null;
            state = VState.DESTROYED;
            return true;
        }
        return false;
    }

    public enum VState {
        CREATED,
        INITIALIZED,
        DESTROYED,
    }
}
