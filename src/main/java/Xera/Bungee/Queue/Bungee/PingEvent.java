package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PingEvent implements Listener {
    XeraBungeeQueue plugin;

    public PingEvent(XeraBungeeQueue plugin) {
        this.plugin = plugin;
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
            ServerPing.PlayerInfo[] info = {};
            int i = 0;

            for (String str : Config.SERVERPINGINFO) {
                info = addInfo(info, new ServerPing.PlayerInfo(XeraBungeeQueue.parseText(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(str))), String.valueOf(i)));

                i++;
            }

            players = new ServerPing.Players(Config.QUEUESERVERSLOTS, plugin.getProxy().getOnlineCount(), info);
        } else {
            players = event.getResponse().getPlayers();
        }

        ServerPing ping = new ServerPing(protocol, players, event.getResponse().getDescriptionComponent(), event.getResponse().getFaviconObject());
        event.setResponse(ping);
    }

    public static ServerPing.PlayerInfo[] addInfo(ServerPing.PlayerInfo[] arr, ServerPing.PlayerInfo info) {
        int i;

        ServerPing.PlayerInfo[] newArray = new ServerPing.PlayerInfo[arr.length + 1];

        for (i = 0; i < arr.length; i++)
            newArray[i] = arr[i];

        newArray[arr.length] = info;

        return newArray;
    }
}
