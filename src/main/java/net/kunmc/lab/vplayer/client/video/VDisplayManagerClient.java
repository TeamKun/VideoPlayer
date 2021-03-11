package net.kunmc.lab.vplayer.client.video;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.kunmc.lab.vplayer.common.model.DisplayManagaer;
import net.kunmc.lab.vplayer.common.model.LifecycleDisplay;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VDisplayManagerClient implements DisplayManagaer<UUID, VDisplayClient> {
    private final Map<UUID, VDisplayClient> displayMap = new ConcurrentHashMap<>();
    private final Deque<VDisplayClient> addQueue = new ArrayDeque<>();
    private final List<VDisplayClient> displays = new ArrayList<>();

    @Nonnull
    @Override
    public VDisplayClient create(UUID uuid) {
        VDisplayClient display = new VDisplayClient(uuid);
        Optional.ofNullable(displayMap.put(uuid, display)).ifPresent(LifecycleDisplay::destroy);
        addQueue.add(display);
        return display;
    }

    @Override
    public VDisplayClient get(UUID uuid) {
        return displayMap.get(uuid);
    }

    @Override
    public void destroy(UUID uuid) {
        Optional.ofNullable(displayMap.remove(uuid)).ifPresent(VDisplayClient::destroy);
    }

    @Override
    public void clear() {
        displays.forEach(VDisplayClient::destroy);
        displayMap.clear();
    }

    @Override
    public List<VDisplayClient> list() {
        return Collections.unmodifiableList(displays);
    }

    public void render(MatrixStack stack) {
        {
            VRendererClient.beginRenderFrame();

            displays.forEach(display -> {
                if (display.canSee())
                    display.validate();
                else
                    display.invalidate();
            });

            {
                VDisplayClient add;
                while ((add = addQueue.poll()) != null)
                    displays.add(add);
            }

            displays.removeIf(VDisplayClient::processRequest);

            displays.forEach(VDisplayClient::renderFrame);

            VRendererClient.endRenderFrame();
        }

        displays.forEach(display -> display.render(stack));
    }
}
