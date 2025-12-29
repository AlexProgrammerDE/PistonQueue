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

import net.pistonmaster.pistonqueue.shared.queue.LoadBalancingStrategy;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Selects which queue server to send a player to based on the configured load balancing strategy.
 */
public final class QueueServerSelector {
  private final QueueEnvironment environment;
  private final AtomicInteger roundRobinCounter = new AtomicInteger();

  public QueueServerSelector(QueueEnvironment environment) {
    this.environment = Objects.requireNonNull(environment, "environment");
  }

  /**
   * Selects a queue server from the given group based on the configured load balancing strategy.
   *
   * @param group the queue group to select a server from
   * @return the selected queue server name
   * @throws IllegalStateException if no queue servers are configured for the group
   */
  public String selectQueueServer(QueueGroup group) {
    List<String> queueServers = group.getQueueServers();
    if (queueServers.isEmpty()) {
      throw new IllegalStateException("No queue servers configured for group: " + group.getName());
    }

    if (queueServers.size() == 1) {
      return queueServers.getFirst();
    }

    LoadBalancingStrategy strategy = environment.config().queueLoadBalancing();
    return switch (strategy) {
      case ROUND_ROBIN -> selectRoundRobin(queueServers);
      case LEAST_PLAYERS -> selectLeastPlayers(queueServers);
      case RANDOM -> selectRandom(queueServers);
    };
  }

  private String selectRoundRobin(List<String> queueServers) {
    int index = roundRobinCounter.getAndUpdate(current -> (current + 1) % queueServers.size());
    return queueServers.get(index % queueServers.size());
  }

  private String selectLeastPlayers(List<String> queueServers) {
    String selected = queueServers.getFirst();
    int minPlayers = Integer.MAX_VALUE;

    for (String serverName : queueServers) {
      Optional<ServerInfoWrapper> server = environment.plugin().getServer(serverName);
      int playerCount = server
        .map(s -> s.getConnectedPlayers().size())
        .orElse(Integer.MAX_VALUE); // Treat offline servers as "full"

      if (playerCount < minPlayers) {
        minPlayers = playerCount;
        selected = serverName;
      }
    }

    return selected;
  }

  private String selectRandom(List<String> queueServers) {
    int index = ThreadLocalRandom.current().nextInt(queueServers.size());
    return queueServers.get(index);
  }
}
