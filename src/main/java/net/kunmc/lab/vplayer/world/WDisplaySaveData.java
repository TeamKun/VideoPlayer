package net.kunmc.lab.vplayer.world;

import net.kunmc.lab.vplayer.model.DisplayManagaer;
import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.model.Quad;
import net.kunmc.lab.vplayer.patch.VideoPatch;
import net.kunmc.lab.vplayer.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.video.VDisplay;
import net.kunmc.lab.vplayer.video.VDisplayManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.kunmc.lab.vplayer.VideoPlayer.MODID;

public class WDisplaySaveData extends WorldSavedData implements DisplayManagaer<String, VDisplay> {
    private static final String DATA_NAME = MODID + "_displays";

    private final Map<String, UUID> displayNames = new ConcurrentHashMap<>();
    private final VDisplayManager manager = new VDisplayManager();

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

    private void sendToClient(VideoPatchOperation operation, List<VideoPatch> patches) {
        MinecraftForge.EVENT_BUS.post(new VideoPatchEvent.Server.SendToClient(operation, patches));
    }

    private void deleteDisplay(UUID e) {
        if (manager.get(e) != null) {
            manager.destroy(e);

            sendToClient(VideoPatchOperation.DELETE, Collections.singletonList(new VideoPatch(e, null, null)));
        }
    }

    // Operation
    @Override
    public VDisplay create(String name) {
        UUID uuid = UUID.randomUUID();
        deleteDisplay(uuid);
        VDisplay display = manager.create(uuid);
        Optional.ofNullable(displayNames.put(name, uuid))
                .ifPresent(this::deleteDisplay);

        sendToClient(VideoPatchOperation.UPDATE, Collections.singletonList(new VideoPatch(uuid, null, display.fetchState())));

        return display;
    }

    @Override
    public void destroy(String name) {
        Optional.ofNullable(displayNames.remove(name))
                .ifPresent(this::deleteDisplay);
    }

    @Override
    public void clear() {
        manager.clear();
        displayNames.clear();
    }

    @Override
    public VDisplay get(String name) {
        return Optional.ofNullable(displayNames.get(name))
                .map(manager::get).orElse(null);
    }

    public VDisplay get(UUID id) {
        return manager.get(id);
    }

    public void dispatchState(String name, Function<PlayState, PlayState> dispatch) {
        Optional.ofNullable(displayNames.get(name))
                .flatMap(e -> Optional.ofNullable(manager.get(e)))
                .ifPresent(e -> {
                    e.dispatchState(dispatch.apply(e.fetchState()));

                    sendToClient(VideoPatchOperation.UPDATE, Collections.singletonList(new VideoPatch(e.getUUID(), e.getQuad(), e.fetchState())));
                });
    }

    public void setQuad(String name, @Nullable Quad quad) {
        Optional.ofNullable(displayNames.get(name))
                .flatMap(e -> Optional.ofNullable(manager.get(e)))
                .ifPresent(e -> {
                    e.setQuad(quad);

                    sendToClient(VideoPatchOperation.UPDATE, Collections.singletonList(new VideoPatch(e.getUUID(), e.getQuad(), e.fetchState())));
                });
    }

    public List<String> listNames() {
        return new ArrayList<>(displayNames.keySet());
    }

    @Override
    public List<VDisplay> list() {
        return manager.list();
    }

    // WorldSavedData methods
    public static WDisplaySaveData get(ServerWorld world) {
        // The IS_GLOBAL constant is there for clarity, and should be simplified into the right branch.
        DimensionSavedDataManager storage = world.getSavedData();
        return storage.getOrCreate(() -> new WDisplaySaveData(DATA_NAME), DATA_NAME);
    }
}