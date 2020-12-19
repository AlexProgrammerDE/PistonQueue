package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
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
public final class QueueListener implements Listener {
    protected boolean mainOnline = false;
    protected boolean queueOnline = false;
    protected boolean authOnline = false;

    private final XeraBungeeQueue plugin;

    private final List<UUID> veteran = new ArrayList<>();
    private final List<UUID> priority = new ArrayList<>();
    private final List<UUID> regular = new ArrayList<>();

    public QueueListener(XeraBungeeQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if enableauth is false
            if (!Config.AUTHFIRST && (Config.ALWAYSQUEUE || plugin.getProxy().getOnlineCount() > Config.MAINSERVERSLOTS)) {
                if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                    veteran.add(player.getUniqueId());
                } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                    priority.add(player.getUniqueId());
                } else {
                    regular.add(player.getUniqueId());
                }
            }
        } else {
            event.getPlayer().disconnect(ChatUtils.parseToComponent(Config.SERVERDOWNKICKMESSAGE));
        }
    }

    @EventHandler
    public void onQueueSend(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (!Config.AUTHFIRST || player.hasPermission(Config.QUEUEBYPASSPERMISSION))
            return;

        if (event.getFrom() != null &&
                event.getFrom().equals(plugin.getProxy().getServerInfo(Config.AUTHSERVER)) &&
                player.getServer().getInfo().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER))) {

            if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                putQueueAuthFirst(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, XeraBungeeQueue.veteranQueue);
            } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                putQueueAuthFirst(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, XeraBungeeQueue.priorityQueue);
            } else {
                putQueueAuthFirst(player, Config.HEADER, Config.FOOTER, XeraBungeeQueue.regularQueue);
            }
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (Config.AUTHFIRST || player.hasPermission(Config.QUEUEBYPASSPERMISSION))
            return;

        if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
            putQueue(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, XeraBungeeQueue.veteranQueue, veteran,  event);
        } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            putQueue(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, XeraBungeeQueue.priorityQueue, priority, event);
        } else {
            putQueue(player, Config.HEADER, Config.FOOTER, XeraBungeeQueue.regularQueue, regular, event);
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        XeraBungeeQueue.veteranQueue.remove(uuid);
        XeraBungeeQueue.priorityQueue.remove(uuid);
        XeraBungeeQueue.regularQueue.remove(uuid);
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
        if (Config.MAINSERVERSLOTS <= plugin.getProxy().getOnlineCount()
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

    private void connectPlayer(LinkedHashMap<UUID, String> queueMap) {
        Entry<UUID, String> entry = queueMap.entrySet().iterator().next();
        ProxiedPlayer player = plugin.getProxy().getPlayer(entry.getKey());

        queueMap.remove(entry.getKey());

        player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.JOININGMAINSERVER.replaceAll("%server%", entry.getValue())));
        player.connect(plugin.getProxy().getServerInfo(entry.getValue()));
        player.resetTabHeader();
    }

    private void putQueueAuthFirst(ProxiedPlayer player, List<String> header, List<String> footer, LinkedHashMap<UUID, String> queueMap) {
        preQueueAdding(player, header, footer);

        // Store the data concerning the player's original destination
        queueMap.put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(ProxiedPlayer player, List<String> header, List<String> footer, LinkedHashMap<UUID, String> queueMap, List<UUID> queueList, ServerConnectEvent event) {
        if (!queueList.contains(player.getUniqueId()))
            return;

        queueList.remove(player.getUniqueId());

        preQueueAdding(player, header, footer);

        // Redirect the player to the queue.
        String originalTarget = event.getTarget().getName();

        event.setTarget(plugin.getProxy().getServerInfo(Config.QUEUESERVER));

        // Store the data concerning the player's original destination
        queueMap.put(player.getUniqueId(), originalTarget);
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
                    .replaceAll("%position%", "None")
                    .replaceAll("%wait%", "None"));

            if (i != (tab.size() - 1)) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
