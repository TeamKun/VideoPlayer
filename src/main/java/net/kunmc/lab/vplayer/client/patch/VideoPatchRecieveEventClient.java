package net.kunmc.lab.vplayer.client.patch;

import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;

import java.util.List;

public class VideoPatchRecieveEventClient extends VideoPatchEvent {
    public VideoPatchRecieveEventClient(VideoPatchOperation operation, List<VideoPatch> patches) {
        super(operation, patches);
    }
}
