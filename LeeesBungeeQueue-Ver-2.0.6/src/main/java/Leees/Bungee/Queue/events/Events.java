package Leees.Bungee.Queue.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import Leees.Bungee.Queue.QueuePlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.command.CommandBungee;
import net.md_5.bungee.command.PlayerCommand;
import net.md_5.bungee.event.EventHandler;

/**
 * Events
 */
public class Events implements Listener {

    List<UUID> list = new ArrayList<>();
    ServerInfo queue = ProxyServer.getInstance().getServerInfo(Lang.QUEUESERVER);

    @EventHandler
    public void on(PostLoginEvent event) {
        if (ProxyServer.getInstance().getOnlineCount() <= Lang.MAINSERVERSLOTS)
            return;
        if (!event.getPlayer().hasPermission(Lang.QUEUEBYPASSPERMISSION)) {
            // Send the player to the queue
            list.add(event.getPlayer().getUniqueId());
            return;
        }
        // Send the player to the main server

    }

    @EventHandler
    public void onSend(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (!list.contains(player.getUniqueId()))
            return;
        list.remove(player.getUniqueId());
        if (!player.hasPermission(Lang.QUEUEBYPASSPERMISSION)) {
            // Send the player to the queue and send a message.
            String originalTarget = e.getTarget().getName();
            e.setTarget(queue);
            player.setTabHeader(
                    new ComponentBuilder(Lang.HEADER.replace("&", "§").replace("<position>", "None").replace("<wait>", "None")).create(),
                    new ComponentBuilder(Lang.FOOTER.replace("&", "§").replace("<position>", "None").replace("<wait>", "None")).create());
            player.sendMessage(ChatColor.GOLD + Lang.SERVERISFULLMESSAGE.replace("&", "§"));
            // Store the data concerning the player's destination
            QueuePlugin.final_destination.put(player.getUniqueId(), originalTarget);
            return;
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        list.remove(e.getPlayer().getUniqueId());
        if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(Lang.QUEUESERVER)) {
            e.getPlayer().setReconnectServer(ProxyServer.getInstance()
                    .getServerInfo(QueuePlugin.final_destination.get(e.getPlayer().getUniqueId())));
        }

        QueuePlugin.final_destination.remove(e.getPlayer().getUniqueId());
    }


    public static void moveQueue() {
        if (Lang.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount() - QueuePlugin.final_destination.size())
            return;
        if (QueuePlugin.final_destination.isEmpty())
            return;
        Entry<UUID, String> entry = QueuePlugin.final_destination.entrySet().iterator().next();
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());
        player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
        player.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Lang.JOININGMAINSERVER.replace("&", "§").replace("<server>", entry.getValue())));
        QueuePlugin.final_destination.remove(entry.getKey());

    }

}
