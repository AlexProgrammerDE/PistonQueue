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

import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

/// Handles the bookkeeping required when a player gets placed into a queue.
public final class QueueEntryFactory {
  private final QueueEnvironment environment;
  private final QueueServerSelector queueServerSelector;

  public QueueEntryFactory(QueueEnvironment environment, QueueServerSelector queueServerSelector) {
    this.environment = Objects.requireNonNull(environment, "environment");
    this.queueServerSelector = Objects.requireNonNull(queueServerSelector, "queueServerSelector");
  }

  public void enqueue(PlayerWrapper player, QueueGroup group, QueueType type, PQServerPreConnectEvent event, boolean serverFull, Config config) {
    player.sendPlayerList(type.getHeader(), type.getFooter());

    Optional<String> originalTarget = event.getTarget();
    String selectedQueueServer = queueServerSelector.selectQueueServer(group);
    event.setTarget(selectedQueueServer);

    Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();
    String queueTarget;
    if (config.forceTargetServer() || originalTarget.isEmpty()) {
      queueTarget = environment.defaultTarget(group);
    } else {
      queueTarget = originalTarget.get();
    }

    UUID playerId = player.getUniqueId();
    boolean shouldNotifyFull = false;
    Lock writeLock = type.getQueueLock().writeLock();
    writeLock.lock();
    try {
      if (serverFull && !queueMap.containsKey(playerId)) {
        shouldNotifyFull = true;
      }

      queueMap.putIfAbsent(playerId, new QueueType.QueuedPlayer(queueTarget, QueueType.QueueReason.SERVER_FULL));
    } finally {
      writeLock.unlock();
    }

    if (shouldNotifyFull) {
      player.sendMessage(config.serverIsFullMessage());
    }
  }
}
