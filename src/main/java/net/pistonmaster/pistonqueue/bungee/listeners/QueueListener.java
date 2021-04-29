/*
 * #%L
 * PistonQueue
 * %%
 * Copyright (C) 2021 AlexProgrammerDE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.pistonmaster.pistonqueue.bungee.listeners;

import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public final class QueueListener implements Listener {
    private final PistonQueue plugin;
    @Setter
    public boolean mainOnline = false;
    @Setter
    public boolean queueOnline = false;
    @Setter
    public boolean authOnline = false;
    /**
     * 1 = veteran, 2 = priority, 3 = regular
     */
    private int line = 1;

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (StorageTool.isShadowBanned(player) && plugin.getBanType() == BanType.KICK) {
            event.getPlayer().disconnect(ChatUtils.parseToComponent(Config.SERVERDOWNKICKMESSAGE));
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
            if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if auth is not enabled
                if (Config.ALWAYSQUEUE || isMainFull() || (!mainOnline && !Config.KICKWHENDOWN)) {
                    if (player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                        event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
                    } else {
                        if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
                            putQueue(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, PistonQueue.getVeteranQueue(), event);
                        } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
                            putQueue(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, PistonQueue.getPriorityQueue(), event);
                        } else {
                            putQueue(player, Config.HEADER, Config.FOOTER, PistonQueue.getRegularQueue(), event);
                        }
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
            if (isAuthToQueue(event) && player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                event.getPlayer().connect(plugin.getProxy().getServerInfo(Config.MAINSERVER));
                return;
            }

            // Its null when joining!
            if (event.getFrom() == null) {
                if (Config.ALLOWAUTHSKIP)
                    queuePlayerAuthFirst(player);
            } else if (isAuthToQueue(event)) {
                queuePlayerAuthFirst(player);
            }
        }
    }

    public void queuePlayerAuthFirst(ProxiedPlayer player) {
        if (player.hasPermission(Config.QUEUEVETERANPERMISSION)) {
            putQueueAuthFirst(player, Config.HEADERVETERAN, Config.FOOTERVETERAN, PistonQueue.getVeteranQueue());
        } else if (player.hasPermission(Config.QUEUEPRIORITYPERMISSION)) {
            putQueueAuthFirst(player, Config.HEADERPRIORITY, Config.FOOTERPRIORITY, PistonQueue.getPriorityQueue());
        } else {
            putQueueAuthFirst(player, Config.HEADER, Config.FOOTER, PistonQueue.getRegularQueue());
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

        player.connect(plugin.getProxy().getServerInfo(entry.getValue()), (result, error) -> {
            if (Boolean.FALSE.equals(result)) {
                player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.RECOVERYMESSAGE));
                queueMap.put(entry.getKey(), entry.getValue());
            }
        });
    }

    private void putQueueAuthFirst(ProxiedPlayer player, List<String> header, List<String> footer, Map<UUID, String> queueMap) {
        preQueueAdding(player, header, footer);

        // Store the data concerning the player's original destination
        queueMap.put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(ProxiedPlayer player, List<String> header, List<String> footer, Map<UUID, String> queueMap, ServerConnectEvent event) {
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

    private boolean isAuthToQueue(ServerSwitchEvent event) {
        return event.getFrom() != null && event.getFrom().equals(plugin.getProxy().getServerInfo(Config.AUTHSERVER)) && event.getPlayer().getServer().getInfo().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER));
    }
}
