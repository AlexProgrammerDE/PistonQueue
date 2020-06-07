package Leees.Bungee.Queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Events
 */
public class Events implements Listener {

    List<UUID> regular = new ArrayList<>();
    List<UUID> priority = new ArrayList<>();
    ServerInfo queue = ProxyServer.getInstance().getServerInfo(Lang.QUEUESERVER);

    @EventHandler
    public void onPreLogin(PreLoginEvent ple) {
        if (!ple.getConnection().getName().matches(Lang.REGEX)) {
            ple.setCancelReason(ChatColor.GOLD + "[LBQ] Invalid username please use: " + Lang.REGEX);
            ple.setCancelled(true);
        }
    }

    @EventHandler
    public void on(PostLoginEvent event) {
        if (ProxyServer.getInstance().getOnlineCount() <= Lang.MAINSERVERSLOTS)
            return;
        if (event.getPlayer().hasPermission(Lang.QUEUEPRIORITYPERMISSION)) {
            // Send the priority player to the priority queue
            priority.add(event.getPlayer().getUniqueId());
            return;
        }
        if (!event.getPlayer().hasPermission(Lang.QUEUEBYPASSPERMISSION) && !event.getPlayer().hasPermission(Lang.QUEUEPRIORITYPERMISSION)) {
            // Send the player to the regular queue
            regular.add(event.getPlayer().getUniqueId());
            return;
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();
        if (player.hasPermission(Lang.QUEUEPRIORITYPERMISSION)) {
            if (!priority.contains(player.getUniqueId()))
                return;
            priority.remove(player.getUniqueId());
            // Send the player to the queue and send a message.
            String originalTarget = e.getTarget().getName();
            e.setTarget(queue);
            player.setTabHeader(
                    new ComponentBuilder(Lang.HEADERPRIORITY.replace("&", "§").replace("<position>", "None").replace("<wait>", "None")).create(),
                    new ComponentBuilder(Lang.FOOTERPRIORITY.replace("&", "§").replace("<position>", "None").replace("<wait>", "None")).create());
            player.sendMessage(ChatColor.GOLD + Lang.SERVERISFULLMESSAGE.replace("&", "§"));
            // Store the data concerning the player's destination
            QueuePlugin.priorityqueue.put(player.getUniqueId(), originalTarget);
            return;
        } else if (!e.getPlayer().hasPermission(Lang.QUEUEBYPASSPERMISSION) && !e.getPlayer().hasPermission(Lang.QUEUEPRIORITYPERMISSION)) {
            if (!regular.contains(player.getUniqueId()))
                return;
            regular.remove(player.getUniqueId());
            // Send the player to the queue and send a message.
            String originalTarget = e.getTarget().getName();
            e.setTarget(queue);
            player.setTabHeader(
                    new ComponentBuilder(Lang.HEADER.replace("&", "§").replace("<position>", "None").replace("<wait>", "None")).create(),
                    new ComponentBuilder(Lang.FOOTER.replace("&", "§").replace("<position>", "None").replace("<wait>", "None")).create());
            player.sendMessage(ChatColor.GOLD + Lang.SERVERISFULLMESSAGE.replace("&", "§"));
            // Store the data concerning the player's destination
            QueuePlugin.regularqueue.put(player.getUniqueId(), originalTarget);
            return;
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        //if a play in queue logs off it removes them and their position so when they relog
        //they get sent to the back of the line and have to wait through the queue again yeah
            try {
                if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(Lang.QUEUESERVER)) {
                    e.getPlayer().setReconnectServer(ProxyServer.getInstance()
                            .getServerInfo(QueuePlugin.priorityqueue.get(e.getPlayer().getUniqueId())));
                    e.getPlayer().setReconnectServer(ProxyServer.getInstance()
                            .getServerInfo(QueuePlugin.regularqueue.get(e.getPlayer().getUniqueId())));
                }
                QueuePlugin.priorityqueue.remove(e.getPlayer().getUniqueId());
                QueuePlugin.regularqueue.remove(e.getPlayer().getUniqueId());
                priority.remove(e.getPlayer().getUniqueId());
                regular.remove(e.getPlayer().getUniqueId());
            }
            catch(NullPointerException error) {
            }
    }

    public static void moveQueue() {
        //checks if priority queue is empty if it is a non priority user always gets in
        //if it has people in it then it gives a chance for either a priority or non
        //priority user to get in when someone logs off the main server
        //gets a random number then if the number is less then or equal to the odds set in
        //this config.yml it will add a priority player if its anything above the odds then
        //a non priority player gets added to the main server
        Random rn = new Random();
        for (int i = 0; i < 100; i++) {
            int answer = rn.nextInt(10) + 1;

                if (answer <= Lang.ODDS && !QueuePlugin.priorityqueue.isEmpty()) {
                    if (Lang.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount() - QueuePlugin.regularqueue.size() - QueuePlugin.priorityqueue.size())
                        return;
                    if (QueuePlugin.priorityqueue.isEmpty())
                        return;
                    Entry<UUID, String> entry2 = QueuePlugin.priorityqueue.entrySet().iterator().next();
                    ProxiedPlayer player2 = ProxyServer.getInstance().getPlayer(entry2.getKey());
                    player2.connect(ProxyServer.getInstance().getServerInfo(entry2.getValue()));
                    player2.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Lang.JOININGMAINSERVER.replace("&", "§").replace("<server>", entry2.getValue())));
                    QueuePlugin.priorityqueue.remove(entry2.getKey());
                } else {
                    if (Lang.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount() - QueuePlugin.regularqueue.size() - QueuePlugin.priorityqueue.size())
                        return;
                    if (QueuePlugin.regularqueue.isEmpty())
                        return;
                    Entry<UUID, String> entry = QueuePlugin.regularqueue.entrySet().iterator().next();
                    ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());
                    player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
                    player.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(Lang.JOININGMAINSERVER.replace("&", "§").replace("<server>", entry.getValue())));
                    QueuePlugin.regularqueue.remove(entry.getKey());
                }
        }
    }

    @EventHandler
    public void onkick(ServerKickEvent event) {
        if (Lang.ENABLEKICKMESSAGE.equals("true")) {
            event.setKickReason(Lang.KICKMESSAGE.replace("&", "§"));
        }
    }
}
