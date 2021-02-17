package net.kunmc.lab.videoplayer.videoplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class VManager {
    private final Deque<VDisplay> addQueue = new ArrayDeque<>();

    private final List<VDisplay> displays = new ArrayList<>();

    public List<VDisplay> getDisplays() {
        return displays;
    }

    public void add(VDisplay display) {
        addQueue.add(display);
    }

    public void render(MatrixStack stack) {
        {
            VDisplay add;
            while ((add = addQueue.poll()) != null) {
                add.processRequest();
                displays.add(add);
            }
        }

        {
            VRenderer.beginRenderFrame();
            displays.forEach(VDisplay::renderFrame);
            VRenderer.endRenderFrame();
        }

        displays.forEach(display -> display.render(stack));

        displays.forEach(display -> {
            if (display.canSee())
                display.validate();
            else
                display.invalidate();
        });

        displays.removeIf(VDisplay::processRequest);
    }
}
