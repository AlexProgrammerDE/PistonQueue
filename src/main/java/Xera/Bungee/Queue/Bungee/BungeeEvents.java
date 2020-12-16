package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * BungeeEvents
 */
public class BungeeEvents implements Listener {
    List<UUID> regular = new ArrayList<>();
    List<UUID> priority = new ArrayList<>();
    ServerInfo queue = ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER);
    public static boolean mainOnline = false;
    public static boolean queueOnline = false;
    public static boolean authOnline = false;
    public static XeraBungeeQueue plugin;

    @EventHandler
    public void onPreLogin(PreLoginEvent ple) {
        if (!ple.getConnection().getName().matches(Config.REGEX)) {
            ple.setCancelReason(new TextComponent(ChatColor.GOLD + "[XBQ] Invalid username please use: " + Config.REGEX));
            ple.setCancelled(true);
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (mainOnline && queueOnline && authOnline) { // authOnline is always true if enableauth is false
            if (!Config.AUTHFIRST) {
                if (!Config.ALWAYSQUEUE && ProxyServer.getInstance().getOnlineCount() <= Config.MAINSERVERSLOTS) {
                    return;
                }

                queuePlayer(event.getPlayer());
            }
        } else {
            event.getPlayer().disconnect(
                    new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.SERVERDOWNKICKMESSAGE))).create());
        }
    }

    @EventHandler
    public void onQueueSend(ServerSwitchEvent event) {
        if (Config.AUTHFIRST) {
            if (event.getFrom() == null) {
                return;
            }

            if (event.getFrom().equals(ProxyServer.getInstance().getServerInfo(Config.AUTHSERVER)) && event.getPlayer().getServer().getInfo().equals(queue)) {
                queuePlayer(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent e) {
        ProxiedPlayer player = e.getPlayer();

        if (!Config.AUTHFIRST) {
            if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                if (!priority.contains(player.getUniqueId())) {
                    return;
                }

                priority.remove(player.getUniqueId());

                // Send the player to the queue and send a message.
                String originalTarget = e.getTarget().getName();

                e.setTarget(queue);

                player.setTabHeader(
                        new ComponentBuilder(getNoneString(Config.HEADERPRIORITY)).create(),
                        new ComponentBuilder(getNoneString(Config.FOOTERPRIORITY)).create());

                player.sendMessage(
                        new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.SERVERISFULLMESSAGE))).create());

                // Store the data concerning the player's destination
                XeraBungeeQueue.priorityQueue.put(player.getUniqueId(), originalTarget);
            } else if (!e.getPlayer().hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                if (!regular.contains(player.getUniqueId())) {
                    return;
                }

                regular.remove(player.getUniqueId());

                // Send the player to the queue and send a message.
                String originalTarget = e.getTarget().getName();

                e.setTarget(queue);

                player.setTabHeader(
                        new ComponentBuilder(getNoneString(Config.HEADER)).create(),
                        new ComponentBuilder(getNoneString(Config.FOOTER)).create());
                player.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.SERVERISFULLMESSAGE))).create());

                // Store the data concerning the player's destination
                XeraBungeeQueue.regularQueue.put(player.getUniqueId(), originalTarget);
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        // If a play in queue logs off it removes them and their position so when they log in again
        // they get sent to the back of the line and have to wait through the queue again yeah
        try {
            if (e.getPlayer().getServer().getInfo().getName().equalsIgnoreCase(Config.QUEUESERVER)) {
                e.getPlayer().setReconnectServer(ProxyServer.getInstance().getServerInfo(XeraBungeeQueue.priorityQueue.get(e.getPlayer().getUniqueId())));
                e.getPlayer().setReconnectServer(ProxyServer.getInstance().getServerInfo(XeraBungeeQueue.regularQueue.get(e.getPlayer().getUniqueId())));
            }

            XeraBungeeQueue.priorityQueue.remove(e.getPlayer().getUniqueId());
            XeraBungeeQueue.regularQueue.remove(e.getPlayer().getUniqueId());

            priority.remove(e.getPlayer().getUniqueId());
            regular.remove(e.getPlayer().getUniqueId());
        } catch (NullPointerException ignored) {}
    }

    public static void moveQueue() {
        // checks if priority queue is empty if it is a non priority user always gets in
        // if it has people in it then it gives a chance for either a priority or non
        // priority user to get in when someone logs off the main server
        // gets a random number then if the number is less then or equal to the odds set in
        // this bungeeconfig.yml it will add a priority player if its anything above the odds then
        // a non priority player gets added to the main server
        // Random rn = new Random();
        // for (int i = 0; i < 100; i++) {
        // int answer = rn.nextInt(10) + 1;
        if (XeraBungeeQueue.priorityQueue.isEmpty()) {
            if (Config.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount() - XeraBungeeQueue.regularQueue.size() - XeraBungeeQueue.priorityQueue.size()) {
                return;
            }

            if (XeraBungeeQueue.regularQueue.isEmpty()) {
                return;
            }

            Entry<UUID, String> entry = XeraBungeeQueue.regularQueue.entrySet().iterator().next();
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());

            player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
            player.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(
                    ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.JOININGMAINSERVER)
                            .replace("%server%", entry.getValue()))));

            player.resetTabHeader();

            XeraBungeeQueue.regularQueue.remove(entry.getKey());
        } else {
            if (Config.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount() - XeraBungeeQueue.regularQueue.size() - XeraBungeeQueue.priorityQueue.size()) {
                return;
            }

            if (XeraBungeeQueue.priorityQueue.isEmpty()) {
                return;
            }

            Entry<UUID, String> entry2 = XeraBungeeQueue.priorityQueue.entrySet().iterator().next();
            ProxiedPlayer player2 = ProxyServer.getInstance().getPlayer(entry2.getKey());

            player2.connect(ProxyServer.getInstance().getServerInfo(entry2.getValue()));
            player2.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(
                    ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.JOININGMAINSERVER)
                            .replace("%server%", entry2.getValue()))));

            player2.resetTabHeader();

            XeraBungeeQueue.priorityQueue.remove(entry2.getKey());
        }
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (Config.ENABLEKICKMESSAGE) {
            event.setKickReasonComponent(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.KICKMESSAGE))).create());
        }
    }

    private void queuePlayer(ProxiedPlayer player) {
        if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            // Send the priority player to the priority queue
            priority.add(player.getUniqueId());
        } else if (!player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
            // Send the player to the regular queue
            regular.add(player.getUniqueId());
        }

        if (Config.AUTHFIRST) {
            if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                if (!priority.contains(player.getUniqueId())) {
                    return;
                }

                priority.remove(player.getUniqueId());

                player.setTabHeader(
                        new ComponentBuilder(getNoneString(Config.HEADERPRIORITY)).create(),
                        new ComponentBuilder(getNoneString(Config.FOOTERPRIORITY)).create());

                player.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.SERVERISFULLMESSAGE))).create());

                // Store the data concerning the player's destination
                XeraBungeeQueue.priorityQueue.put(player.getUniqueId(), Config.MAINSERVER);
            } else if (!player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                if (!regular.contains(player.getUniqueId())) {
                    return;
                }

                regular.remove(player.getUniqueId());

                player.setTabHeader(
                        new ComponentBuilder(getNoneString(Config.HEADER)).create(),
                        new ComponentBuilder(getNoneString(Config.FOOTER)).create());
                player.sendMessage(new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.SERVERISFULLMESSAGE))).create());

                // Store the data concerning the player's destination
                XeraBungeeQueue.regularQueue.put(player.getUniqueId(), Config.MAINSERVER);
            }
        }
    }

    private String getNoneString(List<String> tab) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < tab.size(); i++) {
            if (i == (tab.size() - 1)) {
                builder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(tab.get(i))
                        .replace("%position%", "None")
                        .replace("%wait%", "None")));
            } else {
                builder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(tab.get(i))
                        .replace("%position%", "None")
                        .replace("%wait%", "None")))
                        .append("\n");
            }
        }

        return builder.toString();
    }
}
