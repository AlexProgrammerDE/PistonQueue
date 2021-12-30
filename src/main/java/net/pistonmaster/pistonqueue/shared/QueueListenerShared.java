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
package net.pistonmaster.pistonqueue.shared;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerConnectedEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.utils.BanType;
import net.pistonmaster.pistonqueue.shared.utils.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public abstract class QueueListenerShared {
    private final PistonQueueProxy plugin;
    @Setter
    @Getter
    protected boolean mainOnline = false;
    @Setter
    protected boolean queueOnline = false;
    @Setter
    protected boolean authOnline = false;
    @Setter
    protected Instant onlineSince = null;

    protected void onPostLogin(PlayerWrapper player) {
        if (StorageTool.isShadowBanned(player.getUniqueId()) && Config.SHADOW_BAN_TYPE == BanType.KICK) {
            player.disconnect(Config.SERVER_DOWN_KICK_MESSAGE);
        }
    }

    protected void onKick(PQKickedFromServerEvent event) {
        if (Config.IF_MAIN_DOWN_SEND_TO_QUEUE && event.getKickedFrom().equals(Config.MAIN_SERVER)) {
            if (event.getKickReason().isPresent()) {
                for (String str : Config.DOWN_WORD_LIST) {
                    if (!event.getKickReason().get().toLowerCase().contains(str))
                        continue;

                    event.setCancelServer(Config.QUEUE_SERVER);

                    event.getPlayer().sendMessage(Config.IF_MAIN_DOWN_SEND_TO_QUEUE_MESSAGE);

                    QueueType.getQueueType(event.getPlayer()::hasPermission).getQueueMap().put(event.getPlayer().getUniqueId(), event.getKickedFrom());
                    break;
                }
            }
        }

        if (Config.ENABLE_KICK_MESSAGE) {
            event.setKickMessage(Config.KICK_MESSAGE);
        }
    }

    protected void onPreConnect(PQServerPreConnectEvent event) {
        PlayerWrapper player = event.getPlayer();

        if (Config.AUTH_FIRST) {
            if (Config.ALWAYS_QUEUE)
                return;

            if (isAnyoneQueuedOfType(player))
                return;

            if (!isPlayerMainFull(player) && event.getTarget().isPresent() && event.getTarget().get().equals(Config.QUEUE_SERVER))
                event.setTarget(Config.MAIN_SERVER);
        } else {
            if (player.getCurrentServer().isPresent())
                return;

            if ((Config.KICK_WHEN_DOWN && !mainOnline) || !queueOnline || !authOnline) { // authOnline is always true if auth is not enabled
                player.disconnect(Config.SERVER_DOWN_KICK_MESSAGE);
                return;
            }

            if (Config.ALWAYS_QUEUE || isServerFull(player)) {
                if (player.hasPermission(Config.QUEUE_BYPASS_PERMISSION)) {
                    event.setTarget(Config.MAIN_SERVER);
                } else {
                    putQueue(player, event);
                }
            }
        }
    }

    protected void putQueue(PlayerWrapper player, PQServerPreConnectEvent event) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Redirect the player to the queue.
        Optional<String> originalTarget = event.getTarget();

        event.setTarget(Config.QUEUE_SERVER);

        Map<UUID, String> queueMap = type.getQueueMap();

        // Store the data concerning the player's original destination
        if (Config.FORCE_MAIN_SERVER || !originalTarget.isPresent()) {
            queueMap.put(player.getUniqueId(), Config.MAIN_SERVER);
        } else {
            queueMap.put(player.getUniqueId(), originalTarget.get());
        }
    }

    protected void onConnected(PQServerConnectedEvent event) {
        PlayerWrapper player = event.getPlayer();

        if (Config.AUTH_FIRST) {
            if (isAuthToQueue(event) && player.hasPermission(Config.QUEUE_BYPASS_PERMISSION)) {
                player.connect(Config.MAIN_SERVER);
                return;
            }

            // Its null when joining!
            if (!event.getPreviousServer().isPresent() && player.getCurrentServer().isPresent() && player.getCurrentServer().get().equals(Config.QUEUE_SERVER)) {
                if (Config.ALLOW_AUTH_SKIP)
                    putQueueAuthFirst(player);
            } else if (isAuthToQueue(event)) {
                putQueueAuthFirst(player);
            }
        }
    }

    public void putQueueAuthFirst(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Store the data concerning the player's original destination
        type.getQueueMap().put(player.getUniqueId(), Config.MAIN_SERVER);
    }

    protected void preQueueAdding(PlayerWrapper player, List<String> header, List<String> footer) {
        player.sendPlayerListHeaderAndFooter(header, footer);

        if (isServerFull(player))
            player.sendMessage(Config.SERVER_IS_FULL_MESSAGE);
    }

    protected boolean isServerFull(PlayerWrapper player) {
        return isPlayerMainFull(player) || isAnyoneQueuedOfType(player);
    }

    protected boolean isPlayerMainFull(PlayerWrapper player) {
        return isMainFull(QueueType.getQueueType(player::hasPermission));
    }

    protected int getFreeSlots(QueueType type) {
        return type.getReservedSlots() - type.getPlayersWithTypeInMain().get();
    }

    protected boolean isSlotsFull(int slots) {
        return slots <= 0;
    }

    protected boolean isMainFull(QueueType type) {
        return isSlotsFull(getFreeSlots(type));
    }

    protected boolean isAnyoneQueuedOfType(PlayerWrapper player) {
        return !QueueType.getQueueType(player::hasPermission).getQueueMap().isEmpty();
    }

    protected boolean isAuthToQueue(PQServerConnectedEvent event) {
        return event.getPreviousServer().isPresent() && event.getPreviousServer().get().equals(Config.AUTH_SERVER) && event.getServer().equals(Config.QUEUE_SERVER);
    }

    public void moveQueue() {
        for (QueueType type : QueueType.values()) {
            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                Optional<PlayerWrapper> player = plugin.getPlayer(entry.getKey());

                if (!player.isPresent() || !player.get().getCurrentServer().isPresent() || !player.get().getCurrentServer().get().equals(Config.QUEUE_SERVER)) {
                    type.getQueueMap().remove(entry.getKey());
                }
            }
        }

        if (Config.RECOVERY) {
            plugin.getPlayers().forEach(this::doRecovery);
        }

        if (Config.PAUSE_QUEUE_IF_MAIN_DOWN) {
            if (mainOnline) {
                if (onlineSince != null) {
                    if (Duration.between(onlineSince, Instant.now()).getSeconds() >= Config.START_TIME) {
                        onlineSince = null;
                    } else {
                        return;
                    }
                }
            } else {
                return;
            }
        }

        Arrays.stream(QueueType.values()).forEachOrdered(this::connectPlayer);
    }

    protected void doRecovery(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        if (!type.getQueueMap().containsKey(player.getUniqueId()) && player.getCurrentServer().isPresent() && player.getCurrentServer().get().equals(Config.QUEUE_SERVER)) {
            type.getQueueMap().putIfAbsent(player.getUniqueId(), Config.MAIN_SERVER);

            player.sendMessage(Config.RECOVERY_MESSAGE);
        }
    }

    protected void connectPlayer(QueueType type) {
        int freeSlots = getFreeSlots(type);

        if (isSlotsFull(freeSlots))
            return;

        if (freeSlots > Config.MAX_PLAYERS_PER_MOVE)
            freeSlots = Config.MAX_PLAYERS_PER_MOVE;

        for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
            if (isSlotsFull(freeSlots))
                break;

            Optional<PlayerWrapper> player = plugin.getPlayer(entry.getKey());
            if (!player.isPresent()) {
                continue;
            }

            freeSlots--;

            type.getQueueMap().remove(entry.getKey());

            if (Config.SEND_XP_SOUND)
                sendXPSoundToQueueType(type);

            player.get().sendMessage(Config.JOINING_MAIN_SERVER);
            player.get().sendPlayerListHeaderAndFooter(null, null);

            if (StorageTool.isShadowBanned(player.get().getUniqueId())
                    && (Config.SHADOW_BAN_TYPE == BanType.LOOP
                    || (Config.SHADOW_BAN_TYPE == BanType.TEN_PERCENT && new Random().nextInt(100) >= 10)
                    || (Config.SHADOW_BAN_TYPE == BanType.CUSTOM_PERCENT && new Random().nextInt(100) >= Config.CUSTOM_PERCENT_PERCENTAGE))) {
                player.get().sendMessage(Config.SHADOW_BAN_MESSAGE);

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

    protected void sendXPSoundToQueueType(QueueType type) {
        @SuppressWarnings("UnstableApiUsage")
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("xp");

        AtomicInteger counter = new AtomicInteger(0);
        type.getQueueMap().forEach((uuid, server) -> {
            if (counter.incrementAndGet() <= 5) {
                plugin.getPlayer(uuid).flatMap(playerWrapper ->
                        playerWrapper.getCurrentServer().flatMap(plugin::getServer)).ifPresent(serverWrapper ->
                        serverWrapper.sendPluginMessage("piston:queue", out.toByteArray()));
            }
        });
    }

    protected void indexPositionTime() {
        for (QueueType type : QueueType.values()) {
            int position = 0;

            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                Optional<PlayerWrapper> player = plugin.getPlayer(entry.getKey());
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
}
