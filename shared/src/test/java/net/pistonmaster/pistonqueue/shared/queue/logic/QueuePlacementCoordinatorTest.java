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
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class QueuePlacementCoordinatorTest {

  @BeforeAll
  static void initStorage() {
    QueueTestUtils.ensureStorageToolInitialized();
  }

  @Test
  void queuesPlayerWhenServerFull() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    String queueServer = QueueTestUtils.defaultQueueServer(config);
    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer, queueServer));
    TestPlayer player = context.plugin().registerPlayer("Alice");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, targetServer);

    context.coordinator().handlePreConnect(event);

    assertEquals(queueServer, event.getTarget().orElseThrow());
    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains(config.serverIsFullMessage())));
  }

  @Test
  void queuesPlayerWhenShadowBanned() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    QueueType type = config.getAllQueueTypes().getFirst();

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("BannedPlayer");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, targetServer));

    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
  }

  @Test
  void bypassesQueueWhenPermissionPresent() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Bob");
    player.grantPermission(config.queueBypassPermission());
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, targetServer);

    context.coordinator().handlePreConnect(event);

    assertEquals(targetServer, event.getTarget().orElseThrow());
    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void queuesEvenWhenNotFullIfAlwaysQueueEnabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    config.setAlwaysQueue(true);
    QueueType type = config.getAllQueueTypes().getFirst();

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Cara");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, targetServer));

    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
  }

  @Test
  void retainsOriginalTargetWhenNotForced() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    config.setForceTargetServer(false);
    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
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
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    config.setForceTargetServer(true);
    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Ethan");
    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, "creative"));

    QueuedPlayer queuedPlayer = type.getQueueMap().get(player.getUniqueId());
    assertNotNull(queuedPlayer);
    assertEquals(targetServer, queuedPlayer.targetServer());
  }

  @Test
  void disconnectsWhenRequiredServerDown() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    config.setKickWhenDown(true);
    config.setRawKickWhenDownServers(List.of("%TARGET_SERVER%"));
    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers());
    TestPlayer player = context.plugin().registerPlayer("Finn");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, targetServer));

    assertTrue(player.isDisconnected());
    assertEquals(config.serverDownKickMessage(), player.getDisconnectMessage());
    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void skipsQueueWhenSourceServerUnsupported() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    config.setEnableSourceServer(true);

    // Rebuild groups with source server
    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of("lobby"));
    groupConfig.setQueueTypes(List.of("DEFAULT"));
    config.setQueueGroupDefinitions(java.util.Map.of("default", groupConfig));

    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Gina");
    player.setCurrentServer("hub");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, targetServer));

    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void queuesWhenSourceMatchesRequirement() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    config.setEnableSourceServer(true);

    // Rebuild groups with source server
    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of("lobby"));
    groupConfig.setQueueTypes(List.of("DEFAULT"));
    config.setQueueGroupDefinitions(java.util.Map.of("default", groupConfig));

    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Henry");
    player.setCurrentServer("lobby");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, targetServer));

    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
  }

  @Test
  void ignoresAlreadyConnectedPlayersWhenSourceDisabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    config.setEnableSourceServer(false);
    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Ivy");
    player.setCurrentServer(targetServer);

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, targetServer));

    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void doesNotDuplicateQueueEntries() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    QueueType type = config.getAllQueueTypes().getFirst();
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Jake");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, targetServer);

    context.coordinator().handlePreConnect(event);
    context.coordinator().handlePreConnect(event);

    assertEquals(1, type.getQueueMap().size());
    long messageCount = player.getMessages().stream().filter(msg -> msg.contains(config.serverIsFullMessage())).count();
    assertEquals(1, messageCount);
  }

  @Test
  void updatesPlayerListWithConfiguredHeader() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(1);
    String targetServer = QueueTestUtils.defaultTargetServer(config);
    QueueType type = config.getAllQueueTypes().getFirst();
    type.setHeader(new ArrayList<>(List.of("header")));
    type.setFooter(new ArrayList<>(List.of("footer")));
    type.getPlayersWithTypeInTarget().set(1);

    CoordinatorContext context = context(config, QueueTestUtils.onlineServers(targetServer));
    TestPlayer player = context.plugin().registerPlayer("Liam");

    context.coordinator().handlePreConnect(QueueTestUtils.preConnectEvent(player, targetServer));

    assertEquals(List.of("header"), player.getPlayerListHeader());
    assertEquals(List.of("footer"), player.getPlayerListFooter());
  }

  private CoordinatorContext context(Config config, Set<String> onlineServers) {
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    QueueServerSelector selector = new QueueServerSelector(environment);
    QueueEntryFactory entryFactory = new QueueEntryFactory(environment, selector);
    Set<String> shadowBannedNames = ConcurrentHashMap.newKeySet();
    shadowBannedNames.add("BannedPlayer");
    ShadowBanService shadowBanService = shadowBannedNames::contains;
    QueuePlacementCoordinator coordinator = new QueuePlacementCoordinator(environment, calculator, entryFactory,shadowBanService);
    return new CoordinatorContext(plugin, environment, coordinator);
  }

  private record CoordinatorContext(
    TestQueuePlugin plugin,
    QueueEnvironment environment,
    QueuePlacementCoordinator coordinator
  ) {
  }
}
