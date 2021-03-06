package net.kunmc.lab.vplayer.server.video;

import net.kunmc.lab.vplayer.common.data.DataSerializer;
import net.kunmc.lab.vplayer.common.model.DisplayManagaer;
import net.kunmc.lab.vplayer.common.model.PlayState;
import net.kunmc.lab.vplayer.common.model.Quad;
import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;
import net.kunmc.lab.vplayer.common.video.VDisplay;
import net.kunmc.lab.vplayer.common.video.VDisplayManager;
import net.kunmc.lab.vplayer.server.patch.VideoPatchSendEventServer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.kunmc.lab.vplayer.VideoPlayer.MODID;

public class VDisplayManagerServer extends WorldSavedData implements DisplayManagaer<String, VDisplay> {
    private static final String DATA_NAME = MODID + "_displays";

    private final Map<String, UUID> displayNames = new ConcurrentHashMap<>();
    private final VDisplayManager manager = new VDisplayManager();

    // Required constructors
    public VDisplayManagerServer() {
        super(DATA_NAME);
    }

    public VDisplayManagerServer(String s) {
        super(s);
    }

    @Override
    public boolean isDirty() {
        // This is true for every save because play position is changed every seconds.
        return true;
    }

    @Override
    public void read(CompoundNBT nbt) {
        clear();

        ListNBT list = nbt.getList("displays", Constants.NBT.TAG_COMPOUND);
        list.forEach(node -> {
            if (node.getType() != CompoundNBT.TYPE)
                return;
            CompoundNBT tag = (CompoundNBT) node;
            String name = tag.getString("name");
            String data = tag.getString("data");
            if (name.isEmpty() || data.isEmpty())
                return;
            VideoPatch patch = DataSerializer.decode(data, VideoPatch.class);
            if (patch == null)
                return;
            displayNames.put(name, patch.getId());
            VDisplay display = manager.create(patch.getId());
            display.setQuad(patch.getQuad());
            display.dispatchState(patch.getState());
        });
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        ListNBT list = nbt.getList("displays", Constants.NBT.TAG_COMPOUND);
        displayNames.forEach((name, id) -> {
            Optional.ofNullable(manager.get(id)).ifPresent(d -> {
                CompoundNBT tag = new CompoundNBT();
                tag.putString("name", name);
                tag.putString("data", DataSerializer.encode(new VideoPatch(d.getUUID(), d.getQuad(), d.fetchState())));
                list.add(tag);
            });
        });
        nbt.put("displays", list);
        return nbt;
    }

    private void sendToClient(VideoPatchOperation operation, List<VideoPatch> patches) {
        MinecraftForge.EVENT_BUS.post(new VideoPatchSendEventServer(operation, patches));
    }

    private void deleteDisplay(UUID e) {
        if (manager.get(e) != null) {
            manager.destroy(e);

            sendToClient(VideoPatchOperation.DELETE, Collections.singletonList(new VideoPatch(e, null, null)));
        }
    }

    // Operation
    @Nonnull
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
    public static VDisplayManagerServer get(ServerWorld world) {
        // The IS_GLOBAL constant is there for clarity, and should be simplified into the right branch.
        DimensionSavedDataManager storage = world.getSavedData();
        return storage.getOrCreate(() -> new VDisplayManagerServer(DATA_NAME), DATA_NAME);
    }
}