package net.kunmc.lab.videoplayer.videoplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.videoplayer.videoplayer.model.Display;
import net.kunmc.lab.videoplayer.videoplayer.model.DisplayManagaer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VDisplayManager implements DisplayManagaer {
    private final Map<UUID, VDisplay> displayMap = new ConcurrentHashMap<>();
    private final Deque<VDisplay> addQueue = new ArrayDeque<>();
    private final List<VDisplay> displays = new ArrayList<>();

    @Override
    public VDisplay create(UUID uuid) {
        VDisplay display = new VDisplay();
        Optional.ofNullable(displayMap.put(uuid, display)).ifPresent(VDisplay::destroy);
        addQueue.add(display);
        return display;
    }

    @Override
    public VDisplay get(UUID uuid) {
        return displayMap.get(uuid);
    }

    @Override
    public void destroy(UUID uuid) {
        Optional.ofNullable(displayMap.remove(uuid)).ifPresent(VDisplay::destroy);
    }

    @Override
    public void clear() {
        displays.forEach(VDisplay::destroy);
        displayMap.clear();
    }

    @Override
    public List<Display> list() {
        return Collections.unmodifiableList(displays);
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
