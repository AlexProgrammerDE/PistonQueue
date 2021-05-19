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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueue;
import net.pistonmaster.pistonqueue.bungee.utils.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

@RequiredArgsConstructor
public final class QueueListener implements Listener {

    private final PistonQueue plugin;

    @Setter
    @Getter
    private boolean mainOnline = false;

    @Setter
    private boolean queueOnline = false;

    @Setter
    private boolean authOnline = false;

    @Setter
    private Instant onlineSince = null;

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

            if (isAnyoneQueued())
                return;

            if (!isMainFull() && event.getTarget().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER)))
                event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
        } else {
            if (event.getPlayer().getServer() == null) {
                if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if auth is not enabled
                    if (Config.ALWAYSQUEUE || (isMainFull() || isAnyoneQueued()) || (!mainOnline && !Config.KICKWHENDOWN)) {
                        if (player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                            event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
                        } else {
                            putQueue(player, event);
                        }
                    }
                } else {
                    event.getPlayer().disconnect(ChatUtils.parseToComponent(Config.SERVERDOWNKICKMESSAGE));
                }
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
            if (event.getFrom() == null && event.getPlayer().getServer().getInfo().getName().equals(Config.QUEUESERVER)) {
                if (Config.ALLOWAUTHSKIP)
                    putQueueAuthFirst(player);
            } else if (isAuthToQueue(event)) {
                putQueueAuthFirst(player);
            }
        }
    }

    public void moveQueue() {
        for (QueueType type : QueueType.values()) {
            for (Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                ProxiedPlayer player = plugin.getProxy().getPlayer(entry.getKey());

                if (player == null || (player.getServer() != null && !plugin.getProxy().getServerInfo(Config.QUEUESERVER).equals(player.getServer().getInfo()))) {
                    type.getQueueMap().remove(entry.getKey());
                }
            }
        }

        if (Config.RECOVERY) {
            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                QueueType type = QueueType.getQueueType(player);

                if (!type.getQueueMap().containsKey(player.getUniqueId()) && player.getServer() != null && plugin.getProxy().getServerInfo(Config.QUEUESERVER).equals(player.getServer().getInfo())) {
                    QueueType.getQueueType(player).getQueueMap().putIfAbsent(player.getUniqueId(), Config.MAINSERVER);

                    player.sendMessage(ChatUtils.parseToComponent(Config.RECOVERYMESSAGE));
                }
            }
        }

        if (Config.PAUSEQUEUEIFMAINDOWN) {
            if (mainOnline) {
                if (onlineSince != null) {
                    if (Duration.between(onlineSince, Instant.now()).getSeconds() >= Config.STARTTIME) {
                        onlineSince = null;
                    } else {
                        return;
                    }
                }
            } else {
                return;
            }
        }
        // Check if we even have to move.
        if (isMainFull())
            return;

        switch (line) {
            case 1: {
                moveVeteran(true);
                line = 2;
                break;
            }
            case 2: {
                movePriority(true);
                line = 3;
                break;
            }
            case 3: {
                moveRegular();
                line = 1;
                break;
            }
            default: {
                line = 1;
                break;
            }
        }
    }

    private void moveRegular() {
        if (QueueType.REGULAR.getQueueMap().isEmpty()) {
            moveVeteran(false);
        } else {
            connectPlayer(QueueType.REGULAR);
        }
    }

    private void movePriority(boolean canMoveRegular) {
        if (QueueType.PRIORITY.getQueueMap().isEmpty()) {
            if (canMoveRegular)
                moveRegular();
        } else {
            connectPlayer(QueueType.PRIORITY);
        }
    }

    private void moveVeteran(boolean canMoveRegular) {
        if (QueueType.VETERAN.getQueueMap().isEmpty()) {
            movePriority(canMoveRegular);
        } else {
            connectPlayer(QueueType.VETERAN);
        }
    }

    private void connectPlayer(QueueType type) {
        for (Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
            ProxiedPlayer player = plugin.getProxy().getPlayer(entry.getKey());
            if (player == null || !player.isConnected()) {
                continue;
            }

            type.getQueueMap().remove(entry.getKey());

            player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.JOININGMAINSERVER.replace("%server%", entry.getValue())));
            player.resetTabHeader();

            if (StorageTool.isShadowBanned(player)
                    && (plugin.getBanType() == BanType.LOOP
                    || (plugin.getBanType() == BanType.TENPERCENT && new Random().nextInt(100) >= 10))) {
                player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.SHADOWBANMESSAGE));

                type.getQueueMap().put(entry.getKey(), entry.getValue());

                return;
            }

            indexPositionTime();

            List<Pair<Integer, Instant>> cache = type.getPositionCache().get(entry.getKey());
            if (cache != null) {
                cache.forEach(pair -> type.getDurationToPosition().put(pair.getLeft(), Duration.between(pair.getRight(), Instant.now())));
            }

            player.connect(plugin.getProxy().getServerInfo(entry.getValue()));
        }
    }

    public void putQueueAuthFirst(ProxiedPlayer player) {
        QueueType type = QueueType.getQueueType(player);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Store the data concerning the player's original destination
        type.getQueueMap().put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(ProxiedPlayer player, ServerConnectEvent event) {
        QueueType type = QueueType.getQueueType(player);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Redirect the player to the queue.
        String originalTarget = event.getTarget().getName();

        event.setTarget(plugin.getProxy().getServerInfo(Config.QUEUESERVER));

        Map<UUID, String> queueMap = type.getQueueMap();

        // Store the data concerning the player's original destination
        if (Config.FORCEMAINSERVER) {
            queueMap.put(player.getUniqueId(), Config.MAINSERVER);
        } else {
            queueMap.put(player.getUniqueId(), originalTarget);
        }
    }

    private void preQueueAdding(ProxiedPlayer player, List<String> header, List<String> footer) {
        player.setTabHeader(ChatUtils.parseTab(header), ChatUtils.parseTab(footer));

        player.sendMessage(ChatUtils.parseToComponent(Config.SERVERISFULLMESSAGE));
    }

    private boolean isMainFull() {
        return plugin.getProxy().getServerInfo(Config.MAINSERVER).getPlayers().size() >= Config.MAINSERVERSLOTS;
    }

    private boolean isAuthToQueue(ServerSwitchEvent event) {
        return event.getFrom() != null && event.getFrom().equals(plugin.getProxy().getServerInfo(Config.AUTHSERVER)) && event.getPlayer().getServer().getInfo().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER));
    }

    private boolean isAnyoneQueued() {
        for (QueueType type : QueueType.values()) {
            if (!type.getQueueMap().isEmpty())
                return true;
        }

        return false;
    }

    private void indexPositionTime() {
        for (QueueType type : QueueType.values()) {
            int position = 0;

            for (Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                ProxiedPlayer player = plugin.getProxy().getPlayer(entry.getKey());
                if (player == null || !player.isConnected()) {
                    continue;
                }

                position++;

                if (type.getPositionCache().containsKey(player.getUniqueId())) {
                    List<Pair<Integer, Instant>> list = type.getPositionCache().get(player.getUniqueId());
                    int finalPosition = position;
                    if (list.stream().map(Pair::getLeft).noneMatch(integer -> integer == finalPosition)) {
                        list.add(new Pair<>(position, Instant.now()));
                    }
                } else {
                    List<Pair<Integer, Instant>> list = new ArrayList<>();
                    list.add(new Pair<>(position, Instant.now()));
                    type.getPositionCache().put(player.getUniqueId(), list);
                }
            }
        }
    }
}
