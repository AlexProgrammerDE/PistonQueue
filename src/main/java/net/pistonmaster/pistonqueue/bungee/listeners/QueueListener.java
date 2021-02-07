package net.pistonmaster.pistonqueue.bungee.listeners;

import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueue;
import net.pistonmaster.pistonqueue.bungee.utils.BanType;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.bungee.utils.Config;
import net.pistonmaster.pistonqueue.bungee.utils.StorageTool;

import java.util.*;
import java.util.Map.Entry;

public final class QueueListener implements Listener {
    private final PistonQueue plugin;
    private final List<UUID> veteran = new ArrayList<>();
    private final List<UUID> priority = new ArrayList<>();
    private final List<UUID> regular = new ArrayList<>();
    @Setter
    public boolean mainOnline = false;
    @Setter
    public boolean queueOnline = false;
    @Setter
    public boolean authOnline = false;
    // 1 = veteran, 2 = priority, 3 = regular
    private int line = 1;

    public QueueListener(PistonQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (!Config.AUTHFIRST) {
            if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if enableauth is false
                if (Config.ALWAYSQUEUE || isMainFull()) {
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
    }

    @EventHandler
    public void onQueueSend(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (Config.AUTHFIRST) {
            if (player.hasPermission(Config.QUEUEBYPASSPERMISSION))
                return;

            if (event.getFrom() == null)
                return;

            if (event.getFrom().equals(plugin.getProxy().getServerInfo(Config.AUTHSERVER)) &&
                    player.getServer().getInfo().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER))) {
                if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                    putQueueAuthFirst(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, PistonQueue.getVeteranQueue());
                } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                    putQueueAuthFirst(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, PistonQueue.getPriorityQueue());
                } else {
                    putQueueAuthFirst(player, Config.HEADER, Config.FOOTER, PistonQueue.getRegularQueue());
                }
            }
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (Config.AUTHFIRST) {
            if (Config.ALWAYSQUEUE)
                return;

            if (!isMainFull() && event.getTarget().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER)))
                event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
        } else {
            if (player.hasPermission(Config.QUEUEBYPASSPERMISSION))
                return;

            if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                putQueue(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, PistonQueue.getVeteranQueue(), veteran, event);
            } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                putQueue(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, PistonQueue.getPriorityQueue(), priority, event);
            } else {
                putQueue(player, Config.HEADER, Config.FOOTER, PistonQueue.getRegularQueue(), regular, event);
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        PistonQueue.getVeteranQueue().remove(uuid);
        PistonQueue.getPriorityQueue().remove(uuid);
        PistonQueue.getRegularQueue().remove(uuid);
    }

    public void moveQueue() {
        if (Config.PAUSEQUEUEIFMAINDOWN && !mainOnline) {
            PistonQueue.getVeteranQueue().forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null && player.isConnected())
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
            });

            PistonQueue.getPriorityQueue().forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null && player.isConnected())
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
            });

            PistonQueue.getRegularQueue().forEach((UUID id, String str) -> {
                ProxiedPlayer player = plugin.getProxy().getPlayer(id);

                if (player != null && player.isConnected())
                    player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
            });

            return;
        }

        // Check if we even have to move.
        if (isMainFull())
            return;

        if (line == 1) {
            moveVeteran(true);
            line = 2;
        } else if (line == 2) {
            movePriority(true);
            line = 3;
        } else if (line == 3) {
            moveRegular();
            line = 1;
        } else {
            line = 1;
        }
    }

    private void moveRegular() {
        if (PistonQueue.getRegularQueue().isEmpty()) {
            moveVeteran(false);
        } else {
            connectPlayer(PistonQueue.getRegularQueue());
        }
    }

    private void movePriority(boolean canMoveRegular) {
        if (PistonQueue.getPriorityQueue().isEmpty()) {
            if (canMoveRegular)
                moveRegular();
        } else {
            connectPlayer(PistonQueue.getPriorityQueue());
        }
    }

    private void moveVeteran(boolean canMoveRegular) {
        if (PistonQueue.getVeteranQueue().isEmpty()) {
            movePriority(canMoveRegular);
        } else {
            connectPlayer(PistonQueue.getVeteranQueue());
        }
    }

    private void connectPlayer(Map<UUID, String> queueMap) {
        Optional<Entry<UUID, String>> optional = queueMap.entrySet().stream().findFirst();
        if (!optional.isPresent())
            return;

        Entry<UUID, String> entry = optional.get();
        ProxiedPlayer player = plugin.getProxy().getPlayer(entry.getKey());

        queueMap.remove(entry.getKey());

        if (player == null || !player.isConnected())
            return;

        player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.JOININGMAINSERVER.replace("%server%", entry.getValue())));
        player.resetTabHeader();

        if (StorageTool.isShadowBanned(player)
                && (plugin.getBanType() == BanType.LOOP
                || (plugin.getBanType() == BanType.TENPERCENT && new Random().nextInt(100) >= 10))) {
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
        return plugin.getProxy().getServerInfo(Config.MAINSERVER).getPlayers().size() >= Config.MAINSERVERSLOTS;
    }
}
