package me.l2x9.velocitymotdeditor;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.VelocityServer;
import me.l2x9.velocitymotdeditor.listeners.ProxyPingListener;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Plugin(
        id = "velocitymmotdeditor",
        name = "VelocityMOTDEditor",
        version = "1.0-SNAPSHOT",
        description = "Rewrite the player count and server icon based on domain ",
        authors = {"254n_m"}
)
public class VelocityMOTDEditor {
    private final ProxyServer server;
    private final Logger logger;
    private ConfigurationNode config;
    private File configFile;

    @Inject
    public VelocityMOTDEditor(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        try {
            loadConfig();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new ProxyPingListener((VelocityServer) server, config, loadFavicons()));
    }

    private List<Favicon> loadFavicons() {
        List<Favicon> buf = new ArrayList<>();
        File faviconsFolder = new File(getPluginDataFolder(), "Favicons");
        if (!faviconsFolder.exists()) faviconsFolder.mkdirs();
        for (File file : faviconsFolder.listFiles()) {
            try {
                buf.add(Favicon.create(ImageIO.read(file)));
            } catch (Throwable t) {
                logger.warn("File {} is not a valid favicon", file.getAbsolutePath());
            }
        }
        return buf;
    }

    private void loadConfig() throws Throwable {
        configFile = new File(getPluginDataFolder(), "config.conf");
        if (!configFile.exists()) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("config.conf");
            if (is == null) throw new NullPointerException("Missing resource config.conf");
            Files.copy(is, configFile.toPath());
        }
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setFile(configFile).build();
        config = loader.load();
    }

    private File getPluginDataFolder() {
        File dataFolder = new File("plugins", getClass().getAnnotation(Plugin.class).id());
        if (!dataFolder.exists()) dataFolder.mkdirs();
        return dataFolder;
    }
}
