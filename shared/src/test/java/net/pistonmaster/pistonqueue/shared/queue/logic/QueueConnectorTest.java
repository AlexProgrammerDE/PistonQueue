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
import net.pistonmaster.pistonqueue.shared.queue.BanType;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class QueueConnectorTest {

  @Test
  void calculateEffectiveFreeSlotsReturnsZeroWhenNoSlots() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    Set<String> shadowBannedNames = ConcurrentHashMap.newKeySet();
    ShadowBanService shadowBanService = shadowBannedNames::contains;
    QueueConnector connector = new QueueConnector(environment, calculator, shadowBanService);

    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(5); // Server is full

    int result = connector.calculateEffectiveFreeSlots(config, type);

    assertEquals(0, result);
  }

  @Test
  void calculateEffectiveFreeSlotsRespectsMaxPlayersPerMove() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.MAX_PLAYERS_PER_MOVE = 2;
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    Set<String> shadowBannedNames = ConcurrentHashMap.newKeySet();
    ShadowBanService shadowBanService = shadowBannedNames::contains;
    QueueConnector connector = new QueueConnector(environment, calculator, shadowBanService);

    QueueType type = config.QUEUE_TYPES[0];
    type.getPlayersWithTypeInTarget().set(0); // Server has slots

    int result = connector.calculateEffectiveFreeSlots(config, type);

    assertEquals(2, result);
  }

  @Test
  void shouldSkipPlayerDueToShadowBanReturnsTrueForLoopBan() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.SHADOW_BAN_TYPE = BanType.LOOP;
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    Set<String> shadowBannedNames = ConcurrentHashMap.newKeySet();
    shadowBannedNames.add("BannedPlayer");
    ShadowBanService shadowBanService = shadowBannedNames::contains;
    QueueConnector connector = new QueueConnector(environment, calculator, shadowBanService);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("BannedPlayer");

    boolean result = connector.shouldSkipPlayerDueToShadowBan(config, player);

    assertTrue(result);
  }

  @Test
  void shouldSkipPlayerDueToShadowBanReturnsFalseForNonBannedPlayer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.SHADOW_BAN_TYPE = BanType.LOOP;
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    Set<String> shadowBannedNames = ConcurrentHashMap.newKeySet();
    ShadowBanService shadowBanService = shadowBannedNames::contains;
    QueueConnector connector = new QueueConnector(environment, calculator, shadowBanService);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("GoodPlayer");

    boolean result = connector.shouldSkipPlayerDueToShadowBan(config, player);

    assertFalse(result);
  }

  @Test
  void preparePlayerForConnectionSendsMessagesAndResetsList() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    Set<String> shadowBannedNames = ConcurrentHashMap.newKeySet();
    ShadowBanService shadowBanService = shadowBannedNames::contains;
    QueueConnector connector = new QueueConnector(environment, calculator, shadowBanService);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Prepare");

    connector.preparePlayerForConnection(config, player);

    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains(config.JOINING_TARGET_SERVER)));
    assertTrue(player.getPlayerListHeader().isEmpty() && player.getPlayerListFooter().isEmpty());
  }
}
