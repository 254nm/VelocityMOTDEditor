package me.l2x9.velocitymotdeditor.listeners;

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import me.l2x9.velocitymotdeditor.Utils;
import me.l2x9.velocitymotdeditor.VelocityMOTDEditor;
import net.kyori.adventure.text.TextComponent;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static me.l2x9.velocitymotdeditor.Utils.getInitialPing;
import static me.l2x9.velocitymotdeditor.Utils.pingBackend;

/**
 * @author 254n_m
 * @since 7/15/22/ 7:26 PM
 * This file was created as a part of VelocityMOTDEditor
 */

public class ProxyPingListener {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final List<Favicon> favicons;
    private List<TextComponent> motds;
    private HashSet<String> ips;
    private String backendServer;
    private ServerPing.Players lastPlayers;


    public ProxyPingListener(ConfigurationNode config, List<Favicon> favicons) {
        this.favicons = favicons;
        try {
            motds = config.getNode("motd", "text").getList(TypeToken.of(String.class)).stream().map(Utils::parse).collect(Collectors.toList());
            ips = new HashSet<>(config.getNode("motd", "ips").getList(TypeToken.of(String.class)));
            backendServer = config.getNode("motd", "passthroughBackendServer").getString();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }


    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        String ip = event.getConnection().getVirtualHost().orElse(null).getHostString().toLowerCase();
        VelocityInboundConnection conn = (VelocityInboundConnection) event.getConnection();
        try {

            if (!ips.contains(ip)) {
                ServerPing resp;
                try {
                    resp = getInitialPing(conn).get(1, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    resp = event.getPing();
                }
                event.setPing(resp);
                return;
            }
            if (motds.size() == 0 || favicons.size() == 0) return;

            ServerPing originalResp = event.getPing();

            TextComponent randomMOTD = motds.get(random.nextInt(motds.size()));
            Favicon randomFavicon = favicons.get(random.nextInt(favicons.size()));
            try {
                ServerPing backendPing = pingBackend(conn, backendServer).get(1, TimeUnit.SECONDS);
                ServerPing.Players backendPlayers = backendPing.getPlayers().orElseThrow(TimeoutException::new);
                lastPlayers = new ServerPing.Players(backendPlayers.getOnline(), originalResp.getPlayers().get().getMax(), backendPlayers.getSample());
            } catch (TimeoutException | InterruptedException ex) {
                VelocityMOTDEditor.getInstance().log("&e[WARNING]&r&c Failed to ping the backend server&r&a %s", backendServer);
            }
            ServerPing response = new ServerPing(originalResp.getVersion(), lastPlayers, randomMOTD, randomFavicon);
            event.setPing(response);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
