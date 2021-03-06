package net.kunmc.lab.vplayer.world;

import net.kunmc.lab.vplayer.model.PlayState;
import net.kunmc.lab.vplayer.video.VPlayStateStore;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.WorldSavedData;

import java.util.List;

import static net.kunmc.lab.vplayer.VideoPlayer.MODID;

public class WDisplaySaveData extends WorldSavedData {
    private static final String DATA_NAME = MODID + "_displays";

    private List<String, VPlayStateStore>

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

    // WorldSavedData methods
    public static WDisplaySaveData get(ServerWorld world) {
        // The IS_GLOBAL constant is there for clarity, and should be simplified into the right branch.
        DimensionSavedDataManager storage = world.getSavedData();
        return storage.getOrCreate(() -> new WDisplaySaveData(DATA_NAME), DATA_NAME);
    }
}