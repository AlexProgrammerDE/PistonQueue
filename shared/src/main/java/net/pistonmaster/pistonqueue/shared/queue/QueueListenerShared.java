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

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class QueueListenerShared {
  private final PistonQueuePlugin plugin;
  @Getter
  private final ServerStatusManager serverStatusManager;
  private final QueueEnvironment queueEnvironment;
  private final QueuePlacementCoordinator queuePlacementCoordinator;
  private final QueueMoveProcessor queueMoveProcessor;
  private final UsernameValidator usernameValidator;
  private final ShadowBanKickHandler shadowBanKickHandler;
  private final KickEventHandler kickEventHandler;

  protected QueueListenerShared(PistonQueuePlugin plugin) {
    this(prepare(plugin));
  }

  private QueueListenerShared(PreparedListener prepared) {
    this.plugin = prepared.plugin();
    this.serverStatusManager = prepared.serverStatusManager();
    this.queueEnvironment = prepared.queueEnvironment();
    this.queuePlacementCoordinator = prepared.queuePlacementCoordinator();
    this.queueMoveProcessor = prepared.queueMoveProcessor();
    this.usernameValidator = prepared.usernameValidator();
    this.shadowBanKickHandler = prepared.shadowBanKickHandler();
    this.kickEventHandler = prepared.kickEventHandler();
  }

  private static PreparedListener prepare(PistonQueuePlugin plugin) {
    Objects.requireNonNull(plugin, "plugin");
    Config config = Objects.requireNonNull(plugin.getConfiguration(), "configuration");

    ServerStatusManager serverStatusManager = new ServerStatusManager(plugin::getConfiguration);
    QueueEnvironment queueEnvironment = new QueueEnvironment(plugin, plugin::getConfiguration, serverStatusManager::getOnlineServers);
    UsernameValidator usernameValidator = new UsernameValidator(config);
    ShadowBanKickHandler shadowBanKickHandler = new ShadowBanKickHandler(config);

    QueueServerSelector queueServerSelector = new QueueServerSelector(queueEnvironment);
    KickEventHandler kickEventHandler = new KickEventHandler(config, queueEnvironment, queueServerSelector);

    QueueAvailabilityCalculator availabilityCalculator = new QueueAvailabilityCalculator();
    QueueEntryFactory queueEntryFactory = new QueueEntryFactory(queueEnvironment, queueServerSelector);
    ShadowBanService shadowBanService = new StorageShadowBanService();
    QueuePlacementCoordinator queuePlacementCoordinator = new QueuePlacementCoordinator(queueEnvironment, availabilityCalculator, queueEntryFactory,shadowBanService);
    QueueCleaner queueCleaner = new QueueCleaner(queueEnvironment);
    QueueRecoveryHandler recoveryHandler = new QueueRecoveryHandler(queueEnvironment);
    QueueConnector queueConnector = new QueueConnector(queueEnvironment, availabilityCalculator, shadowBanService);
    QueueMoveProcessor queueMoveProcessor = new QueueMoveProcessor(queueEnvironment, queueCleaner, recoveryHandler, queueConnector);
    return new PreparedListener(plugin, serverStatusManager, queueEnvironment, queuePlacementCoordinator, queueMoveProcessor, usernameValidator, shadowBanKickHandler, kickEventHandler);
  }

  private record PreparedListener(PistonQueuePlugin plugin, ServerStatusManager serverStatusManager, QueueEnvironment queueEnvironment, QueuePlacementCoordinator queuePlacementCoordinator, QueueMoveProcessor queueMoveProcessor, UsernameValidator usernameValidator, ShadowBanKickHandler shadowBanKickHandler, KickEventHandler kickEventHandler) {}

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

}
