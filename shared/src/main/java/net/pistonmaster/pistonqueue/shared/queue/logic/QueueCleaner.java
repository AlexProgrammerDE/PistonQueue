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

import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Removes stale entries from the queue maps to prevent memory leaks.
 */
public final class QueueCleaner {
  private final QueueEnvironment environment;

  public QueueCleaner(QueueEnvironment environment) {
    this.environment = Objects.requireNonNull(environment, "environment");
  }

  public void cleanGroup(QueueGroup group) {
    for (QueueType type : group.getQueueTypes()) {
      Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();
      List<UUID> queueSnapshot;
      synchronized (queueMap) {
        queueSnapshot = new ArrayList<>(queueMap.keySet());
      }

      if (queueSnapshot.isEmpty()) {
        continue;
      }

      List<UUID> staleEntries = new ArrayList<>();
      for (UUID uuid : queueSnapshot) {
        Optional<PlayerWrapper> player = environment.plugin().getPlayer(uuid);
        Optional<String> optionalTarget = player.flatMap(PlayerWrapper::getCurrentServer);
        if (optionalTarget.isEmpty() || !optionalTarget.get().equals(group.getQueueServer())) {
          staleEntries.add(uuid);
        }
      }

      if (!staleEntries.isEmpty()) {
        synchronized (queueMap) {
          staleEntries.forEach(queueMap::remove);
        }
      }
    }
  }
}
