package net.kunmc.lab.vplayer.common.video;

import net.kunmc.lab.vplayer.common.model.DisplayManagaer;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VDisplayManager implements DisplayManagaer<UUID, VDisplay> {
    private final Map<UUID, VDisplay> displayMap = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public VDisplay create(UUID uuid) {
        VDisplay display = new VDisplay(uuid);
        Optional.ofNullable(displayMap.put(uuid, display)).ifPresent(VDisplay::destroy);
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
        displayMap.forEach((uuid, display) -> display.destroy());
        displayMap.clear();
    }

    @Override
    public List<VDisplay> list() {
        return new ArrayList<>(displayMap.values());
    }
}
