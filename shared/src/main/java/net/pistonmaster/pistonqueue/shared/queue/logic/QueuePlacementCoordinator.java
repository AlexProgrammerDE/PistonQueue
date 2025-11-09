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

import java.util.Objects;
import java.util.Optional;

/**
 * Encapsulates the pre-connect logic so that it can be unit tested.
 */
public final class QueuePlacementCoordinator {
  private final QueueEnvironment environment;
  private final QueueAvailabilityCalculator availabilityCalculator;
  private final QueueEntryFactory queueEntryFactory;

  public QueuePlacementCoordinator(
    QueueEnvironment environment,
    QueueAvailabilityCalculator availabilityCalculator,
    QueueEntryFactory queueEntryFactory
  ) {
    this.environment = Objects.requireNonNull(environment, "environment");
    this.availabilityCalculator = Objects.requireNonNull(availabilityCalculator, "availabilityCalculator");
    this.queueEntryFactory = Objects.requireNonNull(queueEntryFactory, "queueEntryFactory");
  }

  public void handlePreConnect(PQServerPreConnectEvent event) {
    PlayerWrapper player = event.getPlayer();
    Config config = environment.config();
    QueueGroup targetGroup = event.getTarget()
      .flatMap(name -> config.findGroupByTarget(name))
      .orElse(environment.defaultGroup());

    if (config.ENABLE_SOURCE_SERVER && !isSourceToTarget(event, targetGroup)) {
      return;
    }

    if (!config.ENABLE_SOURCE_SERVER && player.getCurrentServer().isPresent()) {
      return;
    }

    if (config.KICK_WHEN_DOWN) {
      for (String server : config.KICK_WHEN_DOWN_SERVERS) {
        if (!environment.onlineServers().contains(server)) {
          player.disconnect(config.SERVER_DOWN_KICK_MESSAGE);
          return;
        }
      }
    }

    QueueType type = config.getQueueType(player);
    QueueGroup typeGroup = environment.resolveGroupForType(type);

    boolean serverFull = false;
    if (config.ALWAYS_QUEUE || (serverFull = availabilityCalculator.isServerFull(type))) {
      if (player.hasPermission(config.QUEUE_BYPASS_PERMISSION)) {
        event.setTarget(environment.defaultTarget(typeGroup));
      } else {
        queueEntryFactory.enqueue(player, typeGroup, type, event, serverFull, config);
      }
    }
  }

  private boolean isSourceToTarget(PQServerPreConnectEvent event, QueueGroup group) {
    Optional<String> previousServer = event.getPlayer().getCurrentServer();
    return previousServer.isPresent()
      && group.getSourceServers().contains(previousServer.get())
      && event.getTarget().isPresent()
      && group.getTargetServers().contains(event.getTarget().get());
  }
}
