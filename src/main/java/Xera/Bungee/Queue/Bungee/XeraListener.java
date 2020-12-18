package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

public final class XeraListener implements Listener {
    XeraBungeeQueue plugin;

    public XeraListener(XeraBungeeQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent ple) {
        if (!ple.getConnection().getName().matches(Config.REGEX)) {
            ple.setCancelReason(
                    new TextComponent(ChatColor.translateAlternateColorCodes('&', Config.REGEXMESSAGE.replaceAll("%regex%", Config.REGEX))));
            ple.setCancelled(true);
        }
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (Config.ENABLEKICKMESSAGE) {
            event.setKickReasonComponent(
                    new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.KICKMESSAGE))).create());
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing.Protocol protocol;
        ServerPing.Players players;

        if (Config.CUSTOMPROTOCOLENABLE) {
            ServerPing.Protocol provided = event.getResponse().getVersion();

            provided.setName(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.CUSTOMPROTOCOL)));

            protocol = provided;
        } else {
            protocol = event.getResponse().getVersion();
        }

        if (Config.SERVERPINGINFOENABLE) {
            List<ServerPing.PlayerInfo> info = new ArrayList<>();

            Config.SERVERPINGINFO.forEach(str -> {
                info.add(
                        new ServerPing.PlayerInfo(
                                XeraBungeeQueue.parseText(
                                        ChatColor.translateAlternateColorCodes('&',
                                                XeraBungeeQueue.parseText(str))),
                                String.valueOf(Config.SERVERPINGINFO.indexOf(str) -1)));
            });

            players = new ServerPing.Players(Config.QUEUESERVERSLOTS, plugin.getProxy().getOnlineCount(), info.toArray(new ServerPing.PlayerInfo[0]));
        } else {
            players = event.getResponse().getPlayers();
        }

        ServerPing ping = new ServerPing(protocol, players, event.getResponse().getDescriptionComponent(), event.getResponse().getFaviconObject());
        event.setResponse(ping);
    }
}
