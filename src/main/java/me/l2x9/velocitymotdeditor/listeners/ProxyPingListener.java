package me.l2x9.velocitymotdeditor.listeners;

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PingPassthroughMode;
import com.velocitypowered.proxy.connection.client.HandshakeSessionHandler;
import com.velocitypowered.proxy.connection.client.StatusSessionHandler;
import com.velocitypowered.proxy.connection.util.ServerListPingHandler;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import me.l2x9.velocitymotdeditor.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import ninja.leaping.configurate.ConfigurationNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author 254n_m
 * @since 7/15/22/ 7:26 PM
 * This file was created as a part of VelocityMOTDEditor
 */

public class ProxyPingListener {

    private final VelocityServer server;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final List<Favicon> favicons;
    private Method attemptPingPassthroughM;
    private List<TextComponent> motds;
    private HashSet<String> ips;
    private Field inboundF;
    private Field connectionF;

    public ProxyPingListener(VelocityServer server, ConfigurationNode config, List<Favicon> favicons) {
        this.server = server;
        this.favicons = favicons;
        try {
            motds = config.getNode("motd", "text").getList(TypeToken.of(String.class)).stream().map(this::parse).collect(Collectors.toList());
            ips = new HashSet<>(config.getNode("motd", "ips").getList(TypeToken.of(String.class)));
            attemptPingPassthroughM = ServerListPingHandler.class.getDeclaredMethod("attemptPingPassthrough", VelocityInboundConnection.class, PingPassthroughMode.class, List.class, ProtocolVersion.class);
            attemptPingPassthroughM.setAccessible(true);
            inboundF = StatusSessionHandler.class.getDeclaredField("inbound");
            inboundF.setAccessible(true);
            connectionF = StatusSessionHandler.class.getDeclaredField("connection");
            connectionF.setAccessible(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private TextComponent parse(String str) {
        return Component.text(Utils.translateColorCodes(str.replace("%nl", "\n")));
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        String ip = event.getConnection().getVirtualHost().orElse(null).getHostString().toLowerCase();
        if (ips.contains(ip)) {
            ServerPing originalResp = event.getPing();
            if (motds.size() == 0 || favicons.size() == 0) return;
            ServerPing response = new ServerPing(
                    originalResp.getVersion(),
                    originalResp.getPlayers().get(),
                    motds.get(random.nextInt(0, motds.size())),
                    favicons.get(random.nextInt(0, favicons.size())));
            event.setPing(response);
        } else {
            try {
                VelocityInboundConnection conn = (VelocityInboundConnection) event.getConnection();
                event.setPing(getInitialPing(conn).get());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public CompletableFuture<ServerPing> getInitialPing(VelocityInboundConnection connection) throws Throwable {
        ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion()) ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
        String virtualHostStr = connection.getVirtualHost().map(InetSocketAddress::getHostString).map(String::toLowerCase).orElse("");
        List<String> serversToTry = this.server.getConfiguration().getForcedHosts().getOrDefault(virtualHostStr, this.server.getConfiguration().getAttemptConnectionOrder());
        return (CompletableFuture<ServerPing>) attemptPingPassthroughM.invoke(server.getServerListPingHandler(), connection, PingPassthroughMode.ALL, serversToTry, shownVersion);
    }
}
