package net.kunmc.lab.vplayer;

import net.kunmc.lab.vplayer.proxy.PClient;
import net.kunmc.lab.vplayer.proxy.PCommon;
import net.kunmc.lab.vplayer.proxy.PServer;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(VideoPlayer.MODID)
public class VideoPlayer {

    public static final String MODID = "vplayer";

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public VideoPlayer() {
        PCommon proxy = DistExecutor.safeRunForDist(() -> PClient::new, () -> PServer::new);
        proxy.registerEvents();
    }

}
