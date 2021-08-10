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
package net.pistonmaster.pistonqueue.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.pistonmaster.pistonqueue.utils.BanType;
import net.pistonmaster.pistonqueue.utils.Config;
import net.pistonmaster.pistonqueue.utils.Pair;
import net.pistonmaster.pistonqueue.utils.QueueType;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;
import net.pistonmaster.pistonqueue.velocity.utils.ChatUtils;
import net.pistonmaster.pistonqueue.velocity.utils.StorageTool;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RequiredArgsConstructor
public class QueueListener {
    private final PistonQueueVelocity plugin;

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

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if (StorageTool.isShadowBanned(player) && plugin.getBanType() == BanType.KICK) {
            event.getPlayer().disconnect(ChatUtils.parseToComponent(Config.SERVERDOWNKICKMESSAGE));
        }
    }

    @Subscribe
    public void onSend(ServerPreConnectEvent event) {
        Player player = event.getPlayer();

        if (Config.AUTHFIRST) {
            if (Config.ALWAYSQUEUE)
                return;

            if (isAnyoneQueuedOfType(player))
                return;

            if (!isPlayersQueueFull(player) && event.getResult().getServer().get().equals(plugin.getServer().getServer(Config.QUEUESERVER).get()))
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getServer().getServer(Config.MAINSERVER).get()));
        } else {
            if (!event.getPlayer().getCurrentServer().isPresent()) {
                if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if auth is not enabled
                    if (Config.ALWAYSQUEUE || (isPlayersQueueFull(player) || isAnyoneQueuedOfType(player)) || (!mainOnline && !Config.KICKWHENDOWN)) {
                        if (player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                            event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getServer().getServer(Config.MAINSERVER).get()));
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

    @Subscribe
    public void onQueueSend(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        if (Config.AUTHFIRST) {
            if (isAuthToQueue(event) && player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                event.getPlayer().createConnectionRequest(plugin.getServer().getServer(Config.MAINSERVER).get());
                return;
            }

            // Its null when joining!
            if (!event.getPreviousServer().isPresent() && event.getPlayer().getCurrentServer().get().getServerInfo().getName().equals(Config.QUEUESERVER)) {
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
            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                Optional<Player> player = plugin.getServer().getPlayer(entry.getKey());

                if (!player.isPresent() || (player.get().getCurrentServer().isPresent() && !plugin.getServer().getServer(Config.QUEUESERVER).get().equals(player.get().getCurrentServer().get().getServer()))) {
                    type.getQueueMap().remove(entry.getKey());
                }
            }
        }

        if (Config.RECOVERY) {
            for (Player player : plugin.getServer().getAllPlayers()) {
                QueueType type = QueueType.getQueueType(player::hasPermission);

                if (!type.getQueueMap().containsKey(player.getUniqueId()) && player.getCurrentServer().isPresent() && plugin.getServer().getServer(Config.QUEUESERVER).get().equals(player.getCurrentServer().get().getServer())) {
                    type.getQueueMap().putIfAbsent(player.getUniqueId(), Config.MAINSERVER);

                    if (!noRecoveryMessage.contains(player.getUniqueId())) {
                        noRecoveryMessage.remove(player.getUniqueId());
                        player.sendMessage(ChatUtils.parseToComponent(Config.RECOVERYMESSAGE));
                    }
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

        for (QueueType type : QueueType.values()) {
            if (!isQueueFull(type)) {
                connectPlayer(type);
            }
        }
    }

    private void connectPlayer(QueueType type) {
        for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
            Optional<Player> player = plugin.getServer().getPlayer(entry.getKey());
            if (!player.isPresent()) {
                continue;
            }

            type.getQueueMap().remove(entry.getKey());

            player.get().sendMessage(ChatUtils.parseToComponent(Config.JOININGMAINSERVER.replace("%server%", entry.getValue())));
            player.get().sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());

            if (StorageTool.isShadowBanned(player.get())
                    && (plugin.getBanType() == BanType.LOOP
                    || (plugin.getBanType() == BanType.TENPERCENT && new Random().nextInt(100) >= 10))) {
                player.get().sendMessage(ChatUtils.parseToComponent(Config.SHADOWBANMESSAGE));

                type.getQueueMap().put(entry.getKey(), entry.getValue());

                return;
            }

            indexPositionTime();

            List<Pair<Integer, Instant>> cache = type.getPositionCache().get(entry.getKey());
            if (cache != null) {
                cache.forEach(pair -> type.getDurationToPosition().put(pair.getLeft(), Duration.between(pair.getRight(), Instant.now())));
            }

            player.get().createConnectionRequest(plugin.getServer().getServer(entry.getValue()).get());
        }
    }

    public void putQueueAuthFirst(Player player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Store the data concerning the player's original destination
        type.getQueueMap().put(player.getUniqueId(), Config.MAINSERVER);
    }

    private void putQueue(Player player, ServerPreConnectEvent event) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Redirect the player to the queue.
        String originalTarget = event.getResult().getServer().get().getServerInfo().getName();

        event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getServer().getServer(Config.QUEUESERVER).get()));

        Map<UUID, String> queueMap = type.getQueueMap();

        // Store the data concerning the player's original destination
        if (Config.FORCEMAINSERVER) {
            queueMap.put(player.getUniqueId(), Config.MAINSERVER);
        } else {
            queueMap.put(player.getUniqueId(), originalTarget);
        }
    }

    private void preQueueAdding(Player player, List<String> header, List<String> footer) {
        player.sendPlayerListHeaderAndFooter(ChatUtils.parseTab(header), ChatUtils.parseTab(footer));

        player.sendMessage(ChatUtils.parseToComponent(Config.SERVERISFULLMESSAGE));
    }

    private boolean isPlayersQueueFull(Player player) {
        return isQueueFull(QueueType.getQueueType(player::hasPermission));
    }

    private boolean isQueueFull(QueueType type) {
        return type.getPlayersWithTypeInMain() >= type.getReservatedSlots();
    }

    private boolean isAuthToQueue(ServerConnectedEvent event) {
        return event.getPreviousServer().isPresent() && event.getPreviousServer().get().equals(plugin.getServer().getServer(Config.AUTHSERVER).get()) && event.getServer().equals(plugin.getServer().getServer(Config.QUEUESERVER).get());
    }

    private boolean isAnyoneQueuedOfType(Player player) {
        return !QueueType.getQueueType(player::hasPermission).getQueueMap().isEmpty();
    }

    private void indexPositionTime() {
        for (QueueType type : QueueType.values()) {
            int position = 0;

            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                Optional<Player> player = plugin.getServer().getPlayer(entry.getKey());
                if (!player.isPresent()) {
                    continue;
                }

                position++;

                if (type.getPositionCache().containsKey(player.get().getUniqueId())) {
                    List<Pair<Integer, Instant>> list = type.getPositionCache().get(player.get().getUniqueId());
                    int finalPosition = position;
                    if (list.stream().map(Pair::getLeft).noneMatch(integer -> integer == finalPosition)) {
                        list.add(new Pair<>(position, Instant.now()));
                    }
                } else {
                    List<Pair<Integer, Instant>> list = new ArrayList<>();
                    list.add(new Pair<>(position, Instant.now()));
                    type.getPositionCache().put(player.get().getUniqueId(), list);
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
                plugin.getLogger().error("Had to hotfix queue " + type.name() + "!!! Report this directly to the plugins developer!!!");
            }
        }
    }
}
