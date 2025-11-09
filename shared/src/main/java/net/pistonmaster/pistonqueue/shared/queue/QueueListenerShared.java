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
package net.pistonmaster.pistonqueue.shared.queue;

import lombok.Getter;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;
import net.pistonmaster.pistonqueue.shared.events.PQPreLoginEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueAvailabilityCalculator;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueCleaner;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueConnector;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueEntryFactory;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueEnvironment;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueMoveProcessor;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueuePlacementCoordinator;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueRecoveryHandler;
import net.pistonmaster.pistonqueue.shared.queue.logic.ShadowBanService;
import net.pistonmaster.pistonqueue.shared.queue.logic.StorageShadowBanService;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public abstract class QueueListenerShared {
  private final PistonQueuePlugin plugin;
  @Getter
  private final Set<String> onlineServers = Collections.synchronizedSet(new HashSet<>());
  private final QueueEnvironment queueEnvironment;
  private final QueuePlacementCoordinator queuePlacementCoordinator;
  private final QueueMoveProcessor queueMoveProcessor;

  protected QueueListenerShared(PistonQueuePlugin plugin) {
    this.plugin = plugin;
    this.queueEnvironment = new QueueEnvironment(plugin, this::currentConfig, onlineServers);
    QueueAvailabilityCalculator availabilityCalculator = new QueueAvailabilityCalculator();
    QueueEntryFactory queueEntryFactory = new QueueEntryFactory(queueEnvironment);
    this.queuePlacementCoordinator = new QueuePlacementCoordinator(queueEnvironment, availabilityCalculator, queueEntryFactory);
    QueueCleaner queueCleaner = new QueueCleaner(queueEnvironment);
    QueueRecoveryHandler recoveryHandler = new QueueRecoveryHandler(queueEnvironment);
    ShadowBanService shadowBanService = new StorageShadowBanService();
    QueueConnector queueConnector = new QueueConnector(queueEnvironment, availabilityCalculator, shadowBanService);
    this.queueMoveProcessor = new QueueMoveProcessor(queueEnvironment, queueCleaner, recoveryHandler, queueConnector);
  }

  protected void onPreLogin(PQPreLoginEvent event) {
    if (event.isCancelled())
      return;

    Config config = currentConfig();
    if (config.ENABLE_USERNAME_REGEX && !event.getUsername().matches(config.USERNAME_REGEX)) {
      event.setCancelled(config.USERNAME_REGEX_MESSAGE.replace("%regex%", config.USERNAME_REGEX));
    }
  }

  protected void onPostLogin(PlayerWrapper player) {
    Config config = currentConfig();
    if (StorageTool.isShadowBanned(player.getName()) && config.SHADOW_BAN_TYPE == BanType.KICK) {
      player.disconnect(config.SERVER_DOWN_KICK_MESSAGE);
    }
  }

  protected void onKick(PQKickedFromServerEvent event) {
    Config config = currentConfig();
    QueueGroup group = queueEnvironment.resolveGroupForTarget(event.getKickedFrom());
    boolean kickedFromProtectedTarget = group.getTargetServers().contains(event.getKickedFrom());
    if (config.IF_TARGET_DOWN_SEND_TO_QUEUE && kickedFromProtectedTarget) {
      String kickReason = event.getKickReason()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .orElse("unknown reason");

      config.DOWN_WORD_LIST.stream()
        .filter(word -> kickReason.contains(word.toLowerCase(Locale.ROOT)))
        .findFirst()
        .ifPresent(word -> {
          event.setCancelServer(group.getQueueServer());

          event.getPlayer().sendMessage(config.IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE);

          config.getQueueType(event.getPlayer())
            .getQueueMap()
            .put(event.getPlayer().getUniqueId(), new QueueType.QueuedPlayer(event.getKickedFrom(), QueueType.QueueReason.SERVER_DOWN));
        });
    }

    if (config.ENABLE_KICK_MESSAGE && event.willDisconnect()) {
      event.setKickMessage(config.KICK_MESSAGE);
    }
  }

  protected void onPreConnect(PQServerPreConnectEvent event) {
    queuePlacementCoordinator.handlePreConnect(event);
  }

  public void moveQueue() {
    queueMoveProcessor.processQueues();
  }

  private Config currentConfig() {
    return plugin.getConfiguration();
  }
}
