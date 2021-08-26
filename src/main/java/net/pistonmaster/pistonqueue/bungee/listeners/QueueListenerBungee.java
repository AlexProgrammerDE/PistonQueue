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
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueueBungee;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.shared.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

@RequiredArgsConstructor
public final class QueueListenerBungee extends QueueListenerShared implements Listener {
    private final PistonQueueBungee plugin;

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (StorageTool.isShadowBanned(player.getUniqueId()) && Config.SHADOWBANTYPE == BanType.KICK) {
            player.disconnect(ChatUtils.parseToComponent(Config.SERVERDOWNKICKMESSAGE));
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        PlayerWrapper player = plugin.wrapPlayer(event.getPlayer());

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
        PlayerWrapper player = plugin.wrapPlayer(event.getPlayer());

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
            plugin.getProxy().getPlayers().stream().map(plugin::wrapPlayer).forEach(this::doRecovery);
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

    private void connectPlayer(QueueType type) {
        for (Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
            Optional<PlayerWrapper> player = plugin.getPlayer(entry.getKey());
            if (!player.isPresent()) {
                continue;
            }

            type.getQueueMap().remove(entry.getKey());

            player.get().sendMessage(Config.JOININGMAINSERVER);

            player.get().sendPlayerListHeaderAndFooter(null, null);

            if (StorageTool.isShadowBanned(player.get().getUniqueId())
                    && (Config.SHADOWBANTYPE == BanType.LOOP
                    || (Config.SHADOWBANTYPE == BanType.TENPERCENT && new Random().nextInt(100) >= 10))) {

                player.get().sendMessage(Config.SHADOWBANMESSAGE);

                type.getQueueMap().put(entry.getKey(), entry.getValue());

                return;
            }

            indexPositionTime();

            List<Pair<Integer, Instant>> cache = type.getPositionCache().get(entry.getKey());
            if (cache != null) {
                cache.forEach(pair -> type.getDurationToPosition().put(pair.getLeft(), Duration.between(pair.getRight(), Instant.now())));
            }

            player.get().connect(entry.getValue());
        }
    }

    private void putQueue(PlayerWrapper player, ServerConnectEvent event) {
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

    private boolean isAuthToQueue(ServerSwitchEvent event) {
        return event.getFrom() != null && event.getFrom().equals(plugin.getProxy().getServerInfo(Config.AUTHSERVER)) && event.getPlayer().getServer().getInfo().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER));
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
