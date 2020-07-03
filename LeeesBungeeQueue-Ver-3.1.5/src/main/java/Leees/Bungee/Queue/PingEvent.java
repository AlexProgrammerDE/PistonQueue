package Leees.Bungee.Queue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class PingEvent implements Listener {
    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if (Lang.SERVERPINGINFOENABLE.equals("true")) {
            ServerPing.PlayerInfo one = new ServerPing.PlayerInfo((Lang.SERVERPINGINFO.replace("&", "§").replace("%priority%", "" + QueuePlugin.priorityqueue.size()).replace("%regular%", "" + QueuePlugin.regularqueue.size())), "1");
            ServerPing.PlayerInfo[] info = {one};
            ServerPing.Players players = new ServerPing.Players(Lang.QUEUESERVERSLOTS, QueuePlugin.getInstance().getProxy().getOnlineCount(), info);

            ServerPing ping = new ServerPing(event.getResponse().getVersion(), players, event.getResponse().getDescriptionComponent(), event.getResponse().getFaviconObject());
            event.setResponse(ping);
        }
    }
}
