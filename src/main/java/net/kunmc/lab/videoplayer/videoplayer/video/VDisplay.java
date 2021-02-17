package net.kunmc.lab.videoplayer.videoplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import org.apache.commons.lang3.Validate;

import java.util.ArrayDeque;
import java.util.Deque;

public class VDisplay {
    private VQuad quad;
    private VState state = VState.CREATED;
    private VPlayerClient client;
    private boolean destroyRequested;
    private final Deque<String[]> commandQueue = new ArrayDeque<>();

    public void setQuad(VQuad quadIn) {
        quad = quadIn;
    }

    public VQuad getQuad() {
        return quad;
    }

    public VState getState() {
        return state;
    }

    public void init() {
        Validate.validState(state == VState.CREATED, "Invalid State");
        client = new VPlayerClient();
        client.init();
        processCommand();
        state = VState.INITIALIZED;
    }

    public void renderFrame() {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        client.renderFrame();
    }

    public void render(MatrixStack stack) {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        if (quad != null)
            client.render(stack, quad);
    }

    public void destroy() {
        Validate.validState(state == VState.INITIALIZED, "Invalid State");
        destroyRequested = true;
    }

    public void command(String... args) {
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
