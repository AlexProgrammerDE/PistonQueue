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
package net.pistonmaster.pistonqueue.shared.queue.logic;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.BanType;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;

/// Responsible for moving players from the queue onto their target server.
public final class QueueConnector {
  private final QueueEnvironment environment;
  private final QueueAvailabilityCalculator availabilityCalculator;
  private final ShadowBanService shadowBanService;

  public QueueConnector(QueueEnvironment environment, QueueAvailabilityCalculator availabilityCalculator, ShadowBanService shadowBanService) {
    this.environment = Objects.requireNonNull(environment, "environment");
    this.availabilityCalculator = Objects.requireNonNull(availabilityCalculator, "availabilityCalculator");
    this.shadowBanService = Objects.requireNonNull(shadowBanService, "shadowBanService");
  }

  public void connectPlayers(QueueGroup group, QueueType type) {
    Config config = environment.config();
    int freeSlots = calculateEffectiveFreeSlots(config, type);

    if (freeSlots <= 0) {
      return;
    }

    int movesLeft = freeSlots;
    Set<UUID> processedThisCycle = new HashSet<>();

    while (movesLeft > 0) {
      Map.Entry<UUID, QueueType.QueuedPlayer> entry = pollNextQueueEntry(type);
      if (entry == null) {
        break;
      }

      if (!processedThisCycle.add(entry.getKey())) {
        requeuePlayer(type, entry);
        break;
      }

      Optional<PlayerWrapper> optional = environment.plugin().getPlayer(entry.getKey());
      if (optional.isEmpty()) {
        type.getActiveTransfers().remove(entry.getKey());
        continue;
      }
      PlayerWrapper player = optional.get();

      if (shouldSkipPlayerDueToShadowBan(config, player)) {
        player.sendMessage(config.shadowBanMessage());
        requeuePlayer(type, entry);
        continue;
      }

      preparePlayerForConnection(config, player);
      recordPositionDuration(type, entry.getKey());
      connectPlayer(player, entry.getValue().targetServer());
      type.getActiveTransfers().remove(entry.getKey());

      movesLeft--;
    }

    if (config.sendXpSound()) {
      sendXPSoundToQueueType(group, type);
    }
  }

  int calculateEffectiveFreeSlots(Config config, QueueType type) {
    int freeSlots = availabilityCalculator.getFreeSlots(type);
    if (freeSlots <= 0) {
      return 0;
    }
    return Math.min(freeSlots, config.maxPlayersPerMove());
  }

  boolean shouldSkipPlayerDueToShadowBan(Config config, PlayerWrapper player) {
    if (!shadowBanService.isShadowBanned(player.getName())) {
      return false;
    }

    return config.shadowBanType() == BanType.LOOP
      || (config.shadowBanType() == BanType.PERCENT && ThreadLocalRandom.current().nextInt(100) >= config.shadowBanPercent());
  }

  void preparePlayerForConnection(Config config, PlayerWrapper player) {
    player.sendMessage(config.joiningTargetServer());
    player.resetPlayerList();
  }

  void recordPositionDuration(QueueType type, UUID playerId) {
    indexPositionTime(type);

    Map<Integer, Instant> cache = type.getPositionCache().get(playerId);
    if (cache != null) {
      Lock durationWriteLock = type.getDurationLock().writeLock();
      durationWriteLock.lock();
      try {
        cache.forEach((position, instant) ->
          type.getDurationFromPosition().put(position, Duration.between(instant, Instant.now())));
      } finally {
        durationWriteLock.unlock();
      }
    }
  }

  void connectPlayer(PlayerWrapper player, String targetServer) {
    player.connect(targetServer);
  }

  private void sendXPSoundToQueueType(QueueGroup group, QueueType type) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("xpV2");

    List<UUID> uuids = new ArrayList<>(5);
    Lock readLock = type.getQueueLock().readLock();
    readLock.lock();
    try {
      for (UUID uuid : type.getQueueMap().keySet()) {
        uuids.add(uuid);
        if (uuids.size() == 5) {
          break;
        }
      }
    } finally {
      readLock.unlock();
    }

    out.writeInt(uuids.size());
    uuids.forEach(id -> out.writeUTF(id.toString()));

    byte[] data = out.toByteArray();

    // Send XP sound to all queue servers in the group that have players
    for (String queueServer : group.queueServers()) {
      environment.plugin().getServer(queueServer).ifPresent(server -> {
        if (!server.getConnectedPlayers().isEmpty()) {
          server.sendPluginMessage("piston:queue", data);
        }
      });
    }
  }

  private void indexPositionTime(QueueType type) {
    int position = 0;

    Lock readLock = type.getQueueLock().readLock();
    readLock.lock();
    try {
      for (UUID uuid : type.getQueueMap().keySet()) {
        position++;
        Map<Integer, Instant> list = type.getPositionCache().get(uuid);
        if (list == null) {
          type.getPositionCache().put(uuid, new HashMap<>(Collections.singletonMap(position, Instant.now())));
        } else if (!list.containsKey(position)) {
          list.put(position, Instant.now());
        }
      }
    } finally {
      readLock.unlock();
    }
  }

  private Map.Entry<UUID, QueueType.QueuedPlayer> pollNextQueueEntry(QueueType type) {
    Lock writeLock = type.getQueueLock().writeLock();
    writeLock.lock();
    try {
      Iterator<Map.Entry<UUID, QueueType.QueuedPlayer>> iterator = type.getQueueMap().entrySet().iterator();
      if (!iterator.hasNext()) {
        return null;
      }

      Map.Entry<UUID, QueueType.QueuedPlayer> entry = iterator.next();
      iterator.remove();
      type.getActiveTransfers().add(entry.getKey());
      return new AbstractMap.SimpleEntry<>(entry);
    } finally {
      writeLock.unlock();
    }
  }

  private void requeuePlayer(QueueType type, Map.Entry<UUID, QueueType.QueuedPlayer> entry) {
    Lock writeLock = type.getQueueLock().writeLock();
    writeLock.lock();
    try {
      type.getActiveTransfers().remove(entry.getKey());
      type.getQueueMap().put(entry.getKey(), entry.getValue());
    } finally {
      writeLock.unlock();
    }
  }
}
