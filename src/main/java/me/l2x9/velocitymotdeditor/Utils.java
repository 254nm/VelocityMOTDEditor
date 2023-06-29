package me.l2x9.velocitymotdeditor;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PingPassthroughMode;
import com.velocitypowered.proxy.connection.util.ServerListPingHandler;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author 254n_m
 * @since 7/16/22/ 12:34 AM
 * This file was created as a part of VelocityMOTDEditor
 */
public class Utils {
    private static VelocityServer server;
    private static Method attemptPingPassThroughM;

    static {
        try {
            attemptPingPassThroughM = ServerListPingHandler.class.getDeclaredMethod("attemptPingPassthrough", VelocityInboundConnection.class, PingPassthroughMode.class, List.class, ProtocolVersion.class);
            attemptPingPassThroughM.setAccessible(true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static String translateColorCodes(String textToTranslate) {//100% pasted from bukkit because this isnt in minnimessage for some reason
        char[] chars = textToTranslate.toCharArray();

        for (int i = 0; i < chars.length - 1; ++i) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                chars[i] = 167;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static CompletableFuture<ServerPing> getInitialPing(VelocityInboundConnection connection) throws Throwable {
        String virtualHostStr = connection.getVirtualHost().map(InetSocketAddress::getHostString).map(String::toLowerCase).orElse("");
        List<String> serversToTry = server.getConfiguration().getForcedHosts().getOrDefault(virtualHostStr, server.getConfiguration().getAttemptConnectionOrder());
        return pingBackend(connection, serversToTry.toArray(String[]::new));
    }

    @SuppressWarnings("unchecked") //InteliJ is annoying
    public static CompletableFuture<ServerPing> pingBackend(VelocityInboundConnection connection, String... servers) throws Throwable {
        if (server == null) server = (VelocityServer) VelocityMOTDEditor.getInstance().getServer();
        ProtocolVersion shownVersion = ProtocolVersion.isSupported(connection.getProtocolVersion()) ? connection.getProtocolVersion() : ProtocolVersion.MAXIMUM_VERSION;
        return (CompletableFuture<ServerPing>) attemptPingPassThroughM.invoke(server.getServerListPingHandler(), connection, PingPassthroughMode.ALL, Arrays.asList(servers), shownVersion);

    }
    public static TextComponent parse(String str) {
        return Component.text(Utils.translateColorCodes(str.replace("%nl", "\n")));
    }
}
