package net.kunmc.lab.vplayer.model;

import net.kunmc.lab.vplayer.video.VDisplay;

import java.util.List;
import java.util.UUID;

public interface DisplayManagaer {
    Display create(UUID uuid);

    VDisplay computeIfAbsent(UUID uuid);

    Display get(UUID uuid);

    void destroy(UUID uuid);

    void clear();

    List<Display> list();
}
