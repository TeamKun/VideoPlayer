package net.kunmc.lab.vplayer;

import net.kunmc.lab.vplayer.client.ProxyClient;
import net.kunmc.lab.vplayer.server.ProxyServer;
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
        ProxyServer proxy = DistExecutor.safeRunForDist(() -> ProxyClient::new, () -> ProxyServer::new);
        proxy.registerEvents();
    }

}
