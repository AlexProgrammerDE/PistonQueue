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

import com.velocitypowered.api.event.player.KickedFromServerEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueue;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.bungee.utils.StorageTool;
import net.pistonmaster.pistonqueue.utils.*;

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

    @Getter
    private final List<UUID> noRecoveryMessage = new ArrayList<>();

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

            if (isAnyoneQueuedOfType(player))
                return;

            if (!isPlayersQueueFull(player) && event.getTarget().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER)))
                event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
        } else {
            if (event.getPlayer().getServer() == null) {
                if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if auth is not enabled
                    if (Config.ALWAYSQUEUE || isServerFull(player)) {
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
        hotFixQueue();

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
                doRecovery(player);
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

        for (QueueType type : QueueType.values()) {
            if (!isQueueFull(type)) {
                connectPlayer(type);
            }
        }
    }

    private void doRecovery(ProxiedPlayer player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        if (!type.getQueueMap().containsKey(player.getUniqueId()) && player.getServer() != null && plugin.getProxy().getServerInfo(Config.QUEUESERVER).equals(player.getServer().getInfo())) {
            type.getQueueMap().putIfAbsent(player.getUniqueId(), Config.MAINSERVER);

            if (!noRecoveryMessage.contains(player.getUniqueId())) {
                noRecoveryMessage.remove(player.getUniqueId());

                ChatUtils.sendMessage(player, Config.RECOVERYMESSAGE);
            }
        }
    }

    private void connectPlayer(QueueType type) {
        for (Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
            ProxiedPlayer player = plugin.getProxy().getPlayer(entry.getKey());
            if (player == null || !player.isConnected()) {
                continue;
            }

            type.getQueueMap().remove(entry.getKey());

            ChatUtils.sendMessage(player, Config.JOININGMAINSERVER);

            player.resetTabHeader();

            if (StorageTool.isShadowBanned(player)
                    && (plugin.getBanType() == BanType.LOOP
                    || (plugin.getBanType() == BanType.TENPERCENT && new Random().nextInt(100) >= 10))) {

                ChatUtils.sendMessage(player, Config.SHADOWBANMESSAGE);

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
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Store the data concerning the player's original destination
        type.getQueueMap().put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(ProxiedPlayer player, ServerConnectEvent event) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

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

        if (isServerFull(player)) {
            ChatUtils.sendMessage(player, Config.SERVERISFULLMESSAGE);
        }
    }

    private boolean isServerFull(ProxiedPlayer player) {
        return (isPlayersQueueFull(player) || isAnyoneQueuedOfType(player)) || (!mainOnline && !Config.KICKWHENDOWN);
    }

    private boolean isPlayersQueueFull(ProxiedPlayer player) {
        return isQueueFull(QueueType.getQueueType(player::hasPermission));
    }

    private boolean isQueueFull(QueueType type) {
        return type.getPlayersWithTypeInMain() >= type.getReservatedSlots();
    }

    private boolean isAuthToQueue(ServerSwitchEvent event) {
        return event.getFrom() != null && event.getFrom().equals(plugin.getProxy().getServerInfo(Config.AUTHSERVER)) && event.getPlayer().getServer().getInfo().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER));
    }

    private boolean isAnyoneQueuedOfType(ProxiedPlayer player) {
        return !QueueType.getQueueType(player::hasPermission).getQueueMap().isEmpty();
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

    private void hotFixQueue() {
        for (QueueType type : QueueType.values()) {
            int size = 0;

            for (UUID ignored : type.getQueueMap().keySet()) {
                size++;
            }

            if (size != type.getQueueMap().size()) {
                type.setQueueMap(new LinkedHashMap<>());
                plugin.getLogger().severe("Had to hotfix queue " + type.name() + "!!! Report this directly to the plugins developer!!!");
            }
        }
    }
}
