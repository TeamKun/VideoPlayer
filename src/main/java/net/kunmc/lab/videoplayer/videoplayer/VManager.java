package net.kunmc.lab.videoplayer.videoplayer;

import com.mojang.blaze3d.matrix.MatrixStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

public class VManager {
    private final Supplier<VPlayer.VPlayerClient> clientSupplier;
    private final Deque<VDisplay> addQueue = new ArrayDeque<>();

    private final List<VDisplay> clients = new ArrayList<>();

    public VManager(Supplier<VPlayer.VPlayerClient> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }

    public void add(VDisplay display) {
        addQueue.add(display);
    }

    public void render(MatrixStack stack) {
        {
            VDisplay add;
            while ((add = addQueue.poll()) != null) {
                add.init(clientSupplier);
                clients.add(add);
            }
        }

        clients.forEach(client -> client.render(stack));
        clients.removeIf(VDisplay::processDestroy);
    }

    public List<VDisplay> getClients() {
        return clients;
    }
}
