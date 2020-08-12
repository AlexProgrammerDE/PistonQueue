package Leees.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PingEvent implements Listener {
    ServerPing.Protocol protocol;
    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if (Lang.SERVERPINGINFOENABLE.equals("true")) {
            if (!Lang.CUSTOMPROTOCOL.contains("false")) {
                ServerPing.Protocol provided = event.getResponse().getVersion();

                QueuePlugin.getInstance().getLogger().info(String.valueOf(provided.getProtocol()));

                provided.setName(Lang.CUSTOMPROTOCOL.replaceAll("&", "§"));

                protocol = provided;
            } else {
                protocol = event.getResponse().getVersion();
            }
            ServerPing.PlayerInfo[] info = {};
            int i = 0;

            for (String str : Lang.SERVERPINGINFO) {
                info = addInfo(info, new ServerPing.PlayerInfo(str.replaceAll("&", "§").replaceAll("%priority%", "" + QueuePlugin.priorityqueue.size()).replaceAll("%regular%", "" + QueuePlugin.regularqueue.size()), String.valueOf(i)));
                i++;
            }

            ServerPing.Players players = new ServerPing.Players(Lang.QUEUESERVERSLOTS, QueuePlugin.getInstance().getProxy().getOnlineCount(), info);

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
