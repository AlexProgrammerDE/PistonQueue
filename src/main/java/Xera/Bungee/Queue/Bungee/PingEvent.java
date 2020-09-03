package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PingEvent implements Listener {
    ServerPing.Protocol protocol;
    XeraBungeeQueue plugin;

    public PingEvent(XeraBungeeQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if (Lang.SERVERPINGINFOENABLE) {
            if (Lang.CUSTOMPROTOCOLENABLE) {
                ServerPing.Protocol provided = event.getResponse().getVersion();

                plugin.getLogger().info(String.valueOf(provided.getProtocol()));

                provided.setName(Lang.CUSTOMPROTOCOL.replaceAll("&", "§"));

                protocol = provided;
            } else {
                protocol = event.getResponse().getVersion();
            }

            ServerPing.PlayerInfo[] info = {};
            int i = 0;

            for (String str : Lang.SERVERPINGINFO) {
                info = addInfo(info, new ServerPing.PlayerInfo(str.replaceAll("&", "§").replaceAll("%priority%", "" + XeraBungeeQueue.priorityqueue.size()).replaceAll("%regular%", "" + XeraBungeeQueue.regularqueue.size()), String.valueOf(i)));
                i++;
            }

            ServerPing.Players players = new ServerPing.Players(Lang.QUEUESERVERSLOTS, plugin.getProxy().getOnlineCount(), info);

            ServerPing ping = new ServerPing(protocol, players, event.getResponse().getDescriptionComponent(), event.getResponse().getFaviconObject());
            event.setResponse(ping);
        }
    }

    public static ServerPing.PlayerInfo[] addInfo(ServerPing.PlayerInfo[] arr, ServerPing.PlayerInfo info) {
        int i;

        ServerPing.PlayerInfo[] newarr = new ServerPing.PlayerInfo[arr.length + 1];

        for (i = 0; i < arr.length; i++)
            newarr[i] = arr[i];

        newarr[arr.length] = info;

        return newarr;
    }
}
