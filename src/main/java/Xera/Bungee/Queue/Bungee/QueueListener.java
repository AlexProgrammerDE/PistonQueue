package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * ProxyListener
 */
public final class QueueListener implements Listener {
    private final ServerInfo queue = ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER);

    protected boolean mainOnline = false;
    protected boolean queueOnline = false;
    protected boolean authOnline = false;

    private final XeraBungeeQueue plugin;

    public QueueListener(XeraBungeeQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if enableauth is false
            if (!Config.AUTHFIRST && (Config.ALWAYSQUEUE || plugin.getProxy().getOnlineCount() > Config.MAINSERVERSLOTS)) {
                queuePlayer(event.getPlayer());
            }
        } else {
            event.getPlayer().disconnect(ChatUtils.parseToComponent(Config.SERVERDOWNKICKMESSAGE));
        }
    }

    @EventHandler
    public void onQueueSend(ServerSwitchEvent event) {
        if (Config.AUTHFIRST &&
                event.getFrom() != null &&
                event.getFrom().equals(ProxyServer.getInstance().getServerInfo(Config.AUTHSERVER)) &&
                event.getPlayer().getServer().getInfo().equals(queue))
            queuePlayer(event.getPlayer());
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        if (Config.AUTHFIRST || event.getPlayer().hasPermission(Config.QUEUEBYPASSPERMISSION))
            return;

        ProxiedPlayer player = event.getPlayer();

        if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
            putQueue(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, XeraBungeeQueue.veteranQueue, event);
        } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            putQueue(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, XeraBungeeQueue.priorityQueue, event);
        } else {
            putQueue(player, Config.HEADER, Config.FOOTER, XeraBungeeQueue.regularQueue, event);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        XeraBungeeQueue.veteranQueue.remove(event.getPlayer().getUniqueId());
        XeraBungeeQueue.priorityQueue.remove(event.getPlayer().getUniqueId());
        XeraBungeeQueue.regularQueue.remove(event.getPlayer().getUniqueId());
    }

    protected void moveQueue() {
        if (Config.PAUSEQUEUEIFMAINDOWN && !mainOnline) {
            XeraBungeeQueue.veteranQueue.forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null) {
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
                }
            });

            XeraBungeeQueue.priorityQueue.forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null) {
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
                }
            });

            XeraBungeeQueue.regularQueue.forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null) {
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
                }
            });

            return;
        }

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

    private void queuePlayer(ProxiedPlayer player) {
        if (player.hasPermission(Config.QUEUEBYPASSPERMISSION))
            return;

        if (Config.AUTHFIRST) {
            if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                putQueueAuthFirst(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, XeraBungeeQueue.veteranQueue);
            } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                putQueueAuthFirst(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, XeraBungeeQueue.priorityQueue);
            } else {
                putQueueAuthFirst(player, Config.HEADER, Config.FOOTER, XeraBungeeQueue.regularQueue);
            }
        }
    }

    private void connectPlayer(LinkedHashMap<UUID, String> queueList) {
        Entry<UUID, String> entry = queueList.entrySet().iterator().next();
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(entry.getKey());

        player.connect(ProxyServer.getInstance().getServerInfo(entry.getValue()));
        player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.JOININGMAINSERVER
                        .replaceAll("%server%", entry.getValue())));
        player.resetTabHeader();

        queueList.remove(entry.getKey());
    }

    private void putQueueAuthFirst(ProxiedPlayer player, List<String> header, List<String> footer, LinkedHashMap<UUID, String> queueList) {
        preQueueAdding(player, header, footer);

        // Store the data concerning the player's destination
        queueList.put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(ProxiedPlayer player, List<String> header, List<String> footer, LinkedHashMap<UUID, String> queueList, ServerConnectEvent event) {
        preQueueAdding(player, header, footer);

        // Send the player to the queue and send a message.
        String originalTarget = event.getTarget().getName();

        event.setTarget(queue);

        // Store the data concerning the player's destination
        queueList.put(player.getUniqueId(), originalTarget);
    }

    private void preQueueAdding(ProxiedPlayer player, List<String> header, List<String> footer) {
        player.setTabHeader(
                new ComponentBuilder(getNoneString(header)).create(),
                new ComponentBuilder(getNoneString(footer)).create());

        player.sendMessage(ChatUtils.parseToComponent(Config.SERVERISFULLMESSAGE));
    }

    private String getNoneString(List<String> tab) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < tab.size(); i++) {
            builder.append(ChatUtils.parseToString(tab.get(i))
                    .replace("%position%", "None")
                    .replace("%wait%", "None"));

            if (i != (tab.size() - 1)) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
