package net.kunmc.lab.vplayer.common.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface DisplayManagaer<ID, D extends Display> {
    @Nonnull
    D create(ID id);

    @Nonnull
    default D computeIfAbsent(ID id) {
        D display = get(id);
        if (display == null)
            display = create(id);
        return display;
    }

    @Nullable
    D get(ID id);

    void destroy(ID id);

    void clear();

    List<D> list();
}
