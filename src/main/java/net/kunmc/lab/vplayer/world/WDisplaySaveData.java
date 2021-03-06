package net.kunmc.lab.vplayer.world;

import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.model.Quad;
import net.kunmc.lab.vplayer.patch.VideoPatch;
import net.kunmc.lab.vplayer.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.video.VDisplay;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.kunmc.lab.vplayer.VideoPlayer.MODID;

public class WDisplaySaveData extends WorldSavedData {
    private static final String DATA_NAME = MODID + "_displays";

    private final Map<String, Pair<UUID, VDisplay>> displayMap = new ConcurrentHashMap<>();

    // Required constructors
    public WDisplaySaveData() {
        super(DATA_NAME);
    }

    public WDisplaySaveData(String s) {
        super(s);
    }

    @Override
    public void read(CompoundNBT nbt) {

    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        return null;
    }

    // Operation
    public void create(String name) {
        UUID uuid = UUID.randomUUID();
        VDisplay display = new VDisplay();
        Optional.ofNullable(displayMap.put(name, Pair.of(uuid, display))).ifPresent(e -> {
            e.getRight().destroy();

            MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Server(VideoPatchOperation.DELETE, Collections.singletonList(new VideoPatch(e.getLeft(), null, null))));
        });

        MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Server(VideoPatchOperation.UPDATE, Collections.singletonList(new VideoPatch(uuid, null, display.fetchState()))));
    }

    public void destroy(String name) {
        Optional.ofNullable(displayMap.remove(name)).ifPresent(e -> {
            e.getRight().destroy();

            MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Server(VideoPatchOperation.DELETE, Collections.singletonList(new VideoPatch(e.getLeft(), null, null))));
        });
    }

    public void dispatchState(String name, Function<PlayState, PlayState> dispatch) {
        Optional.ofNullable(displayMap.get(name)).ifPresent(e -> {
            VDisplay display = e.getRight();
            display.dispatchState(dispatch.apply(display.fetchState()));

            MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Server(VideoPatchOperation.UPDATE, Collections.singletonList(new VideoPatch(e.getLeft(), display.getQuad(), display.fetchState()))));
        });
    }

    public void setQuad(String name, @Nullable Quad quad) {
        Optional.ofNullable(displayMap.get(name)).ifPresent(e -> {
            VDisplay display = e.getRight();
            display.setQuad(quad);

            MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Server(VideoPatchOperation.UPDATE, Collections.singletonList(new VideoPatch(e.getLeft(), display.getQuad(), display.fetchState()))));
        });
    }

    public List<String> list() {
        return new ArrayList<>(displayMap.keySet());
    }

    public Map<String, Pair<UUID, VDisplay>> getMap() {
        return displayMap;
    }

    // WorldSavedData methods
    public static WDisplaySaveData get(ServerWorld world) {
        // The IS_GLOBAL constant is there for clarity, and should be simplified into the right branch.
        DimensionSavedDataManager storage = world.getSavedData();
        return storage.getOrCreate(() -> new WDisplaySaveData(DATA_NAME), DATA_NAME);
    }
}