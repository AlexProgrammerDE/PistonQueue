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
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.Objects;
import java.util.Optional;

/**
 * Puts players back into the queue when recovery is enabled and something went wrong with their connection.
 */
public final class QueueRecoveryHandler {
  private final QueueEnvironment environment;

  public QueueRecoveryHandler(QueueEnvironment environment) {
    this.environment = Objects.requireNonNull(environment, "environment");
  }

  public void recoverPlayer(PlayerWrapper player) {
    Config config = environment.config();
    QueueType type = config.getQueueType(player);
    QueueGroup group = environment.resolveGroupForType(type);

    Optional<String> currentServer = player.getCurrentServer();
    if (!type.getQueueMap().containsKey(player.getUniqueId())
      && currentServer.isPresent()
      && currentServer.get().equals(group.getQueueServer())) {
      type.getQueueMap().putIfAbsent(player.getUniqueId(), new QueueType.QueuedPlayer(environment.defaultTarget(group), QueueType.QueueReason.RECOVERY));
      player.sendMessage(config.RECOVERY_MESSAGE);
    }
  }
}
