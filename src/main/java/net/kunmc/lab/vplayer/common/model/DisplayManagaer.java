package net.kunmc.lab.vplayer.common.model;

import java.util.List;

public interface DisplayManagaer<ID, D extends Display> {
    D create(ID id);

    default D computeIfAbsent(ID id) {
        D display = get(id);
        if (display == null)
            display = create(id);
        return display;
    }

    D get(ID id);

    void destroy(ID id);

    void clear();

    List<D> list();
}
