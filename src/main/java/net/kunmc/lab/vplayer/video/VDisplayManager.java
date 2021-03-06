package net.kunmc.lab.vplayer.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.model.Display;
import net.kunmc.lab.vplayer.model.DisplayManagaer;

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
    public VDisplay computeIfAbsent(UUID uuid) {
        VDisplay display = get(uuid);
        if (display == null)
            display = create(uuid);
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
            VRenderer.beginRenderFrame();

            displays.forEach(display -> {
                if (display.canSee())
                    display.validate();
                else
                    display.invalidate();
            });

            {
                VDisplay add;
                while ((add = addQueue.poll()) != null)
                    displays.add(add);
            }

            displays.removeIf(VDisplay::processRequest);

            displays.forEach(VDisplay::renderFrame);

            VRenderer.endRenderFrame();
        }

        displays.forEach(display -> display.render(stack));
    }
}
