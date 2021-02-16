package net.kunmc.lab.videoplayer.videoplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class VManager {
    private final Deque<VDisplay> addQueue = new ArrayDeque<>();

    private final List<VDisplay> clients = new ArrayList<>();

    public void add(VDisplay display) {
        addQueue.add(display);
    }

    public void render(MatrixStack stack) {
        {
            VDisplay add;
            while ((add = addQueue.poll()) != null) {
                add.init();
                clients.add(add);
            }
        }

        {
            VRenderer.beginRenderFrame();
            clients.forEach(VDisplay::renderFrame);
            VRenderer.endRenderFrame();
        }

        clients.forEach(client -> client.render(stack));

        clients.removeIf(VDisplay::processDestroy);
    }

    public List<VDisplay> getClients() {
        return clients;
    }
}
