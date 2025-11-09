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
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.queue.QueueType.QueuedPlayer;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueTestUtils.TestPlayer;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueTestUtils.TestQueuePlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueuePlacementCoordinatorTest {

  @BeforeAll
  static void initStorage() {
    QueueTestUtils.ensureStorageToolInitialized();
  }

  @Test
  void queuesPlayerWhenServerFull() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER, config.QUEUE_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Alice");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER);

    context.coordinator().handlePreConnect(event);

    assertEquals(config.QUEUE_SERVER, event.getTarget().orElseThrow());
    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains(config.SERVER_IS_FULL_MESSAGE)));
  }

  @Test
  void bypassesQueueWhenPermissionPresent() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Bob");
    player.grantPermission(config.QUEUE_BYPASS_PERMISSION);
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER);

    context.coordinator().handlePreConnect(event);

    assertEquals(config.TARGET_SERVER, event.getTarget().orElseThrow());
    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void queuesEvenWhenNotFullIfAlwaysQueueEnabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.ALWAYS_QUEUE = true;
    QueueType type = config.QUEUE_TYPES[0];

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Cara");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER));

    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
  }

  @Test
  void retainsOriginalTargetWhenNotForced() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    config.FORCE_TARGET_SERVER = false;
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Daisy");
    String desiredTarget = "adventure";
    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, desiredTarget));

    QueuedPlayer queuedPlayer = type.getQueueMap().get(player.getUniqueId());
    assertNotNull(queuedPlayer);
    assertEquals(desiredTarget, queuedPlayer.targetServer());
  }

  @Test
  void forcesDefaultTargetWhenFlagEnabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    config.FORCE_TARGET_SERVER = true;
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Ethan");
    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, "creative"));

    QueuedPlayer queuedPlayer = type.getQueueMap().get(player.getUniqueId());
    assertNotNull(queuedPlayer);
    assertEquals(config.TARGET_SERVER, queuedPlayer.targetServer());
  }

  @Test
  void disconnectsWhenRequiredServerDown() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    config.KICK_WHEN_DOWN = true;
    config.KICK_WHEN_DOWN_SERVERS = List.of(config.TARGET_SERVER);
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers());
    TestPlayer player = context.plugin().registerPlayer("Finn");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER));

    assertTrue(player.isDisconnected());
    assertEquals(config.SERVER_DOWN_KICK_MESSAGE, player.getDisconnectMessage());
    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void skipsQueueWhenSourceServerUnsupported() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    config.ENABLE_SOURCE_SERVER = true;
    config.SOURCE_SERVER = "lobby";
    QueueTestUtils.rebuildQueueGroups(config);
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Gina");
    player.setCurrentServer("hub");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER));

    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void queuesWhenSourceMatchesRequirement() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    config.ENABLE_SOURCE_SERVER = true;
    config.SOURCE_SERVER = "lobby";
    QueueTestUtils.rebuildQueueGroups(config);
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Henry");
    player.setCurrentServer("lobby");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER));

    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
  }

  @Test
  void ignoresAlreadyConnectedPlayersWhenSourceDisabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    config.ENABLE_SOURCE_SERVER = false;
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Ivy");
    player.setCurrentServer(config.TARGET_SERVER);

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER));

    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void doesNotDuplicateQueueEntries() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Jake");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER);

    context.coordinator().handlePreConnect(event);
    context.coordinator().handlePreConnect(event);

    assertEquals(1, type.getQueueMap().size());
    long messageCount = player.getMessages().stream().filter(msg -> msg.contains(config.SERVER_IS_FULL_MESSAGE)).count();
    assertEquals(1, messageCount);
  }

  @Test
  void updatesPlayerListWithConfiguredHeader() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    QueueType type = config.QUEUE_TYPES[0];
    type.setHeader(new ArrayList<>(List.of("header")));
    type.setFooter(new ArrayList<>(List.of("footer")));
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(config.TARGET_SERVER));
    TestPlayer player = context.plugin().registerPlayer("Liam");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, config.TARGET_SERVER));

    assertEquals(List.of("header"), player.getPlayerListHeader());
    assertEquals(List.of("footer"), player.getPlayerListFooter());
  }

  private CoordinatorContext context(Config config, Set<String> onlineServers) {
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    QueueEntryFactory entryFactory = new QueueEntryFactory(environment);
    QueuePlacementCoordinator coordinator = new QueuePlacementCoordinator(environment, calculator, entryFactory);
    return new CoordinatorContext(plugin, environment, coordinator);
  }

  private record CoordinatorContext(
    TestQueuePlugin plugin,
    QueueEnvironment environment,
    QueuePlacementCoordinator coordinator
  ) {
  }
}
