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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * ProxyListener
 */
public final class ProxyListener implements Listener {
    private final List<UUID> regular = new ArrayList<>();
    private final List<UUID> priority = new ArrayList<>();
    private final List<UUID> veteran = new ArrayList<>();

    private final ServerInfo queue = ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER);

    protected boolean mainOnline = false;
    protected boolean queueOnline = false;
    protected boolean authOnline = false;

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
            if (!Config.AUTHFIRST && (Config.ALWAYSQUEUE || ProxyServer.getInstance().getOnlineCount() > Config.MAINSERVERSLOTS)) {
                queuePlayer(event.getPlayer());
            }
        } else {
            event.getPlayer().disconnect(
                    new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.SERVERDOWNKICKMESSAGE))).create());
        }
    }

    @EventHandler
    public void onQueueSend(ServerSwitchEvent event) {
        if (Config.AUTHFIRST &&
                event.getFrom() != null &&
                event.getFrom().equals(ProxyServer.getInstance().getServerInfo(Config.AUTHSERVER)) && event.getPlayer().getServer().getInfo().equals(queue))
            queuePlayer(event.getPlayer());
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        if (Config.AUTHFIRST || event.getPlayer().hasPermission(Config.QUEUEBYPASSPERMISSION))
            return;

        ProxiedPlayer player = event.getPlayer();

        if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
            putQueue(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, XeraBungeeQueue.veteranQueue, veteran, event);
        } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            putQueue(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, XeraBungeeQueue.priorityQueue, priority, event);
        } else {
            putQueue(player, Config.HEADER, Config.FOOTER, XeraBungeeQueue.regularQueue, regular, event);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        XeraBungeeQueue.veteranQueue.remove(event.getPlayer().getUniqueId());
        XeraBungeeQueue.priorityQueue.remove(event.getPlayer().getUniqueId());
        XeraBungeeQueue.regularQueue.remove(event.getPlayer().getUniqueId());

        veteran.remove(event.getPlayer().getUniqueId());
        priority.remove(event.getPlayer().getUniqueId());
        regular.remove(event.getPlayer().getUniqueId());
    }

    protected void moveQueue() {
        // Checks if priority queue is empty if it is a non priority user always gets in.
        if (Config.MAINSERVERSLOTS <= ProxyServer.getInstance().getOnlineCount()
                - XeraBungeeQueue.regularQueue.size()
                - XeraBungeeQueue.priorityQueue.size()
                - XeraBungeeQueue.veteranQueue.size())
            return;

        if (XeraBungeeQueue.veteranQueue.isEmpty()) {
            if (XeraBungeeQueue.priorityQueue.isEmpty()) {
                if (!XeraBungeeQueue.regularQueue.isEmpty())
                    connectPlayer(XeraBungeeQueue.regularQueue);
            } else {
                connectPlayer(XeraBungeeQueue.priorityQueue);
            }
        } else {
            connectPlayer(XeraBungeeQueue.veteranQueue);
        }
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (Config.ENABLEKICKMESSAGE) {
            event.setKickReasonComponent(
                    new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.KICKMESSAGE))).create());
        }
    }

    private void queuePlayer(ProxiedPlayer player) {
        if (player.hasPermission(Config.QUEUEBYPASSPERMISSION))
            return;

        if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
            // Send the priority player to the veteran queue
            veteran.add(player.getUniqueId());
        } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            // Send the priority player to the priority queue
            priority.add(player.getUniqueId());
        } else {
            // Send the player to the regular queue
            regular.add(player.getUniqueId());
        }

        if (Config.AUTHFIRST) {
            if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                putQueueAuthFirst(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, XeraBungeeQueue.veteranQueue, veteran);
            } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                putQueueAuthFirst(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, XeraBungeeQueue.priorityQueue, priority);
            } else {
                putQueueAuthFirst(player, Config.HEADER, Config.FOOTER, XeraBungeeQueue.regularQueue, regular);
            }
        }
    }

    private void checkAndRemove(List<UUID> list, ProxiedPlayer player) {
        if (!list.contains(player.getUniqueId()))
            return;

        list.remove(player.getUniqueId());
    }

    private void sendServerFull(ProxiedPlayer player) {
        player.sendMessage(
                new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.SERVERISFULLMESSAGE))).create());
    }

    private void connectPlayer(LinkedHashMap<UUID, String> map) {
        Entry<UUID, String> entry = map.entrySet().iterator().next();
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());

        player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
        player.sendMessage(ChatMessageType.CHAT, TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.JOININGMAINSERVER)
                        .replace("%server%", entry.getValue()))));
        player.resetTabHeader();

        map.remove(entry.getKey());
    }

    private void putQueueAuthFirst(ProxiedPlayer player, List<String> header, List<String> footer, LinkedHashMap<UUID, String> queue2, List<UUID> queue3) {
        preQueueAdding(player, header, footer, queue3);

        // Store the data concerning the player's destination
        queue2.put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(ProxiedPlayer player, List<String> header, List<String> footer, LinkedHashMap<UUID, String> queue2, List<UUID> queue3, ServerConnectEvent event) {
        preQueueAdding(player, header, footer, queue3);

        // Send the player to the queue and send a message.
        String originalTarget = event.getTarget().getName();

        event.setTarget(queue);

        // Store the data concerning the player's destination
        queue2.put(player.getUniqueId(), originalTarget);
    }

    private void preQueueAdding(ProxiedPlayer player, List<String> header, List<String> footer, List<UUID> queue) {
        checkAndRemove(queue, player);

        player.setTabHeader(
                new ComponentBuilder(getNoneString(header)).create(),
                new ComponentBuilder(getNoneString(footer)).create());
        sendServerFull(player);
    }

    private String getNoneString(List<String> tab) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < tab.size(); i++) {
            builder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(tab.get(i))
                    .replace("%position%", "None")
                    .replace("%wait%", "None")));

            if (i != (tab.size() - 1)) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
