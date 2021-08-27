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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

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

    protected void doRecovery(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        if (!type.getQueueMap().containsKey(player.getUniqueId()) && player.getCurrentServer().isPresent() && player.getCurrentServer().get().equals(Config.QUEUESERVER)) {
            type.getQueueMap().putIfAbsent(player.getUniqueId(), Config.MAINSERVER);

            player.sendMessage(Config.RECOVERYMESSAGE);
        }
    }

    public void putQueueAuthFirst(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Store the data concerning the player's original destination
        type.getQueueMap().put(player.getUniqueId(), Config.MAINSERVER);
    }

    protected void preQueueAdding(PlayerWrapper player, List<String> header, List<String> footer) {
        player.sendPlayerListHeaderAndFooter(header, footer);

        player.sendMessage(Config.SERVERISFULLMESSAGE);
    }

    protected boolean isServerFull(PlayerWrapper player) {
        return (isPlayersQueueFull(player) || isAnyoneQueuedOfType(player)) || (!mainOnline && !Config.KICKWHENDOWN);
    }

    protected boolean isPlayersQueueFull(PlayerWrapper player) {
        return isQueueFull(QueueType.getQueueType(player::hasPermission));
    }

    protected boolean isQueueFull(QueueType type) {
        return type.getPlayersWithTypeInMain() >= type.getReservatedSlots();
    }

    protected boolean isAnyoneQueuedOfType(PlayerWrapper player) {
        return !QueueType.getQueueType(player::hasPermission).getQueueMap().isEmpty();
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

    protected void connectPlayer(QueueType type) {
        for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
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

    public void moveQueue() {
        for (QueueType type : QueueType.values()) {
            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                Optional<PlayerWrapper> player = plugin.getPlayer(entry.getKey());

                if (!player.isPresent() || (player.get().getCurrentServer().isPresent() && !player.get().getCurrentServer().get().equals(Config.QUEUESERVER))) {
                    type.getQueueMap().remove(entry.getKey());
                }
            }
        }

        if (Config.RECOVERY) {
            plugin.getPlayers().forEach(this::doRecovery);
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
}