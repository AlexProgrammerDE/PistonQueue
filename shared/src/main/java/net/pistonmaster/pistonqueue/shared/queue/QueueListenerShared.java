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
import net.pistonmaster.pistonqueue.shared.queue.logic.KickEventHandler;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueAvailabilityCalculator;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueCleaner;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueConnector;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueEntryFactory;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueEnvironment;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueMoveProcessor;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueuePlacementCoordinator;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueRecoveryHandler;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueServerSelector;
import net.pistonmaster.pistonqueue.shared.queue.logic.ShadowBanKickHandler;
import net.pistonmaster.pistonqueue.shared.queue.logic.ShadowBanService;
import net.pistonmaster.pistonqueue.shared.queue.logic.StorageShadowBanService;
import net.pistonmaster.pistonqueue.shared.queue.logic.UsernameValidator;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class QueueListenerShared {
  private final PistonQueuePlugin plugin;
  @Getter
  private final Set<String> onlineServers = ConcurrentHashMap.newKeySet();
  private final QueueEnvironment queueEnvironment;
  private final QueuePlacementCoordinator queuePlacementCoordinator;
  private final QueueMoveProcessor queueMoveProcessor;
  private final UsernameValidator usernameValidator;
  private final ShadowBanKickHandler shadowBanKickHandler;
  private final KickEventHandler kickEventHandler;

  protected QueueListenerShared(PistonQueuePlugin plugin) {
    this.plugin = plugin;
    this.queueEnvironment = new QueueEnvironment(plugin, this::currentConfig, onlineServers);
    Config config = currentConfig();
    this.usernameValidator = new UsernameValidator(config);
    this.shadowBanKickHandler = new ShadowBanKickHandler(config);

    QueueServerSelector queueServerSelector = new QueueServerSelector(queueEnvironment);
    this.kickEventHandler = new KickEventHandler(config, queueEnvironment, queueServerSelector);

    QueueAvailabilityCalculator availabilityCalculator = new QueueAvailabilityCalculator();
    QueueEntryFactory queueEntryFactory = new QueueEntryFactory(queueEnvironment, queueServerSelector);
    this.queuePlacementCoordinator = new QueuePlacementCoordinator(queueEnvironment, availabilityCalculator, queueEntryFactory);
    QueueCleaner queueCleaner = new QueueCleaner(queueEnvironment);
    QueueRecoveryHandler recoveryHandler = new QueueRecoveryHandler(queueEnvironment);
    ShadowBanService shadowBanService = new StorageShadowBanService();
    QueueConnector queueConnector = new QueueConnector(queueEnvironment, availabilityCalculator, shadowBanService);
    this.queueMoveProcessor = new QueueMoveProcessor(queueEnvironment, queueCleaner, recoveryHandler, queueConnector);
  }

  protected void onPreLogin(PQPreLoginEvent event) {
    usernameValidator.validateUsername(event);
  }

  protected void onPostLogin(PlayerWrapper player) {
    shadowBanKickHandler.handleShadowBanKick(player);
  }

  protected void onKick(PQKickedFromServerEvent event) {
    kickEventHandler.handleKick(event);
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
