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
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;

import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * Handles kick events and potentially redirects players to the queue.
 */
public final class KickEventHandler {
  private final Config config;
  private final QueueEnvironment queueEnvironment;

  public KickEventHandler(Config config, QueueEnvironment queueEnvironment) {
    this.config = Objects.requireNonNull(config, "config");
    this.queueEnvironment = Objects.requireNonNull(queueEnvironment, "queueEnvironment");
  }

  /**
   * Handles a kick event, potentially redirecting the player to the queue if they were kicked
   * from a protected target server due to it being down.
   *
   * @param event the kick event
   */
  public void handleKick(PQKickedFromServerEvent event) {
    handleQueueRedirection(event);
    handleKickMessage(event);
  }

  private void handleQueueRedirection(PQKickedFromServerEvent event) {
    QueueGroup group = queueEnvironment.resolveGroupForTarget(event.getKickedFrom());
    boolean kickedFromProtectedTarget = group.getTargetServers().contains(event.getKickedFrom());

    if (config.ifTargetDownSendToQueue() && kickedFromProtectedTarget) {
      String kickReason = event.getKickReason()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .orElse("unknown reason");

      config.downWordList().stream()
        .filter(word -> kickReason.contains(word.toLowerCase(Locale.ROOT)))
        .findFirst()
        .ifPresent(word -> {
          event.setCancelServer(group.getQueueServer());
          event.getPlayer().sendMessage(config.ifTargetDownSendToQueueMessage());

          QueueType queueType = config.getQueueType(event.getPlayer());
          Lock writeLock = queueType.getQueueLock().writeLock();
          writeLock.lock();
          try {
            queueType.getQueueMap().put(event.getPlayer().getUniqueId(),
              new QueueType.QueuedPlayer(event.getKickedFrom(), QueueType.QueueReason.SERVER_DOWN));
          } finally {
            writeLock.unlock();
          }
        });
    }
  }

  private void handleKickMessage(PQKickedFromServerEvent event) {
    if (config.enableKickMessage() && event.willDisconnect()) {
      event.setKickMessage(config.kickMessage());
    }
  }
}
