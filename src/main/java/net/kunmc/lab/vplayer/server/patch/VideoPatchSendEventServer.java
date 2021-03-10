package net.kunmc.lab.vplayer.server.patch;

import net.kunmc.lab.vplayer.common.patch.VideoPatch;
import net.kunmc.lab.vplayer.common.patch.VideoPatchEvent;
import net.kunmc.lab.vplayer.common.patch.VideoPatchOperation;

import java.util.List;

public class VideoPatchSendEventServer extends VideoPatchEvent {
    public VideoPatchSendEventServer(VideoPatchOperation operation, List<VideoPatch> patches) {
        super(operation, patches);
    }
}
