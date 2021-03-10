package net.kunmc.lab.vplayer.server.patch;

import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.List;

public class VideoPatchRecieveEventServer extends VideoPatchEvent {
    private ServerPlayerEntity player;

    public VideoPatchRecieveEventServer(VideoPatchOperation operation, List<VideoPatch> patches, ServerPlayerEntity player) {
        super(operation, patches);
        this.player = player;
    }

    public ServerPlayerEntity getPlayer() {
        return player;
    }
}
