package ca.xera.bungee.queue.bungee.listeners;

import ca.xera.bungee.queue.bungee.XeraBungeeQueue;
import ca.xera.bungee.queue.bungee.utils.BanType;
import ca.xera.bungee.queue.bungee.utils.ChatUtils;
import ca.xera.bungee.queue.bungee.utils.Config;
import ca.xera.bungee.queue.bungee.utils.StorageTool;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.Map.Entry;

public final class QueueListener implements Listener {
    public boolean mainOnline = false;
    public boolean queueOnline = false;
    public boolean authOnline = false;

    private final XeraBungeeQueue plugin;

    private final List<UUID> veteran = new ArrayList<>();
    private final List<UUID> priority = new ArrayList<>();
    private final List<UUID> regular = new ArrayList<>();

    // 1 = veteran, 2 = priority, 3 = regular
    private int line = 1;

    public QueueListener(XeraBungeeQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if enableauth is false
            if (!Config.AUTHFIRST && isMainFull()) {
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

        if (Config.AUTHFIRST) {
            if (event.getTarget().getName().equals(Config.QUEUESERVER) && !isMainFull())
                event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
        } else {
            if (player.hasPermission(Config.QUEUEBYPASSPERMISSION))
                return;

            if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                putQueue(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, XeraBungeeQueue.veteranQueue, veteran, event);
            } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                putQueue(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, XeraBungeeQueue.priorityQueue, priority, event);
            } else {
                putQueue(player, Config.HEADER, Config.FOOTER, XeraBungeeQueue.regularQueue, regular, event);
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        XeraBungeeQueue.veteranQueue.remove(uuid);
        XeraBungeeQueue.priorityQueue.remove(uuid);
        XeraBungeeQueue.regularQueue.remove(uuid);
    }

    public void moveQueue() {
        if (Config.PAUSEQUEUEIFMAINDOWN && !mainOnline) {
            XeraBungeeQueue.veteranQueue.forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null)
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
            });

            XeraBungeeQueue.priorityQueue.forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null)
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
            });

            XeraBungeeQueue.regularQueue.forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null)
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
            });

            return;
        }

        int onMainServer = plugin.getProxy().getServerInfo(Config.MAINSERVER).getPlayers().size();

        // Check if we even have to move.
        if (onMainServer >= Config.MAINSERVERSLOTS)
            return;

        if (line == 1) {
            moveVeteran(true);
        } else if (line == 2) {
            movePriority(true);
        } else if (line == 3) {
            moveRegular();
        } else {
            line = 1;
        }

        line++;
    }

    private void moveRegular() {
        if (XeraBungeeQueue.regularQueue.isEmpty()) {
            moveVeteran(false);
        } else {
            connectPlayer(XeraBungeeQueue.regularQueue);
        }
    }

    private void movePriority(boolean canMoveRegular) {
        if (XeraBungeeQueue.priorityQueue.isEmpty()) {
            if (canMoveRegular)
                moveRegular();
        } else {
            connectPlayer(XeraBungeeQueue.priorityQueue);
        }
    }

    private void moveVeteran(boolean canMoveRegular) {
        if (XeraBungeeQueue.veteranQueue.isEmpty()) {
            movePriority(canMoveRegular);
        } else {
            connectPlayer(XeraBungeeQueue.veteranQueue);
        }
    }

    private void connectPlayer(Map<UUID, String> queueMap) {
        Entry<UUID, String> entry = queueMap.entrySet().iterator().next();
        ProxiedPlayer player = plugin.getProxy().getPlayer(entry.getKey());

        queueMap.remove(entry.getKey());

        player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.JOININGMAINSERVER.replace("%server%", entry.getValue())));
        player.resetTabHeader();

        if (StorageTool.isShadowBanned(player)
                && (XeraBungeeQueue.banType == BanType.LOOP
                || (XeraBungeeQueue.banType == BanType.TENPERCENT && new Random().nextInt(100) >= 10))) {
            player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.SHADOWBANMESSAGE));

            queueMap.put(entry.getKey(), entry.getValue());

            return;
        }

        player.connect(plugin.getProxy().getServerInfo(entry.getValue()));
    }

    private void putQueueAuthFirst(ProxiedPlayer player, List<String> header, List<String> footer, Map<UUID, String> queueMap) {
        preQueueAdding(player, header, footer);

        // Store the data concerning the player's original destination
        queueMap.put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(ProxiedPlayer player, List<String> header, List<String> footer, Map<UUID, String> queueMap, List<UUID> queueList, ServerConnectEvent event) {
        if (!queueList.contains(player.getUniqueId()))
            return;

        queueList.remove(player.getUniqueId());

        preQueueAdding(player, header, footer);

        // Redirect the player to the queue.
        String originalTarget = event.getTarget().getName();

        event.setTarget(plugin.getProxy().getServerInfo(Config.QUEUESERVER));

        // Store the data concerning the player's original destination
        if (Config.FORCEMAINSERVER) {
            queueMap.put(player.getUniqueId(), Config.MAINSERVER);
        } else {
            queueMap.put(player.getUniqueId(), originalTarget);
        }
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

    private boolean isMainFull() {
        return Config.ALWAYSQUEUE || plugin.getProxy().getOnlineCount() > Config.MAINSERVERSLOTS;
    }
}
