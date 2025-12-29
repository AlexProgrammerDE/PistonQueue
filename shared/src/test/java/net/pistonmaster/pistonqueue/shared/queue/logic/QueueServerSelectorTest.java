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
import net.pistonmaster.pistonqueue.shared.queue.LoadBalancingStrategy;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueTestUtils.TestQueuePlugin;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueTestUtils.TestServer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueueServerSelectorTest {

  @Test
  void throwsWhenNoQueueServersConfigured() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    QueueGroup emptyGroup = new QueueGroup("empty", List.of(), List.of("main"), List.of(), List.of());

    assertThrows(IllegalStateException.class, () -> selector.selectQueueServer(emptyGroup));
  }

  @Test
  void returnsSingleServerWhenOnlyOneConfigured() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    QueueGroup singleServerGroup = new QueueGroup("single", List.of("queue1"), List.of("main"), List.of(), List.of());

    String selected = selector.selectQueueServer(singleServerGroup);

    assertEquals("queue1", selected);
  }

  @Test
  void roundRobinDistributesEvenly() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.ROUND_ROBIN);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2", "queue3"), List.of("main"), List.of(), List.of());

    Map<String, Integer> counts = new HashMap<>();
    for (int i = 0; i < 9; i++) {
      String selected = selector.selectQueueServer(group);
      counts.merge(selected, 1, Integer::sum);
    }

    // Each server should be selected exactly 3 times
    assertEquals(3, counts.get("queue1"));
    assertEquals(3, counts.get("queue2"));
    assertEquals(3, counts.get("queue3"));
  }

  @Test
  void roundRobinCyclesCorrectly() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.ROUND_ROBIN);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2"), List.of("main"), List.of(), List.of());

    assertEquals("queue1", selector.selectQueueServer(group));
    assertEquals("queue2", selector.selectQueueServer(group));
    assertEquals("queue1", selector.selectQueueServer(group));
    assertEquals("queue2", selector.selectQueueServer(group));
  }

  @Test
  void leastPlayersSelectsServerWithFewestPlayers() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.LEAST_PLAYERS);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    // Register servers with different player counts
    TestServer queue1 = plugin.registerServer("queue1");
    TestServer queue2 = plugin.registerServer("queue2");
    plugin.registerServer("queue3"); // queue3 has no players

    // Add players to servers
    queue1.getConnectedPlayers().add(plugin.registerPlayer("P1"));
    queue1.getConnectedPlayers().add(plugin.registerPlayer("P2"));
    queue1.getConnectedPlayers().add(plugin.registerPlayer("P3"));
    queue2.getConnectedPlayers().add(plugin.registerPlayer("P4"));

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2", "queue3"), List.of("main"), List.of(), List.of());

    String selected = selector.selectQueueServer(group);

    assertEquals("queue3", selected);
  }

  @Test
  void leastPlayersSelectsFirstServerOnTie() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.LEAST_PLAYERS);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    // Register servers with same player count (0) - just register, we don't need the return values
    plugin.registerServer("queue1");
    plugin.registerServer("queue2");
    plugin.registerServer("queue3");

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2", "queue3"), List.of("main"), List.of(), List.of());

    String selected = selector.selectQueueServer(group);

    // Should select first server when all are equal
    assertEquals("queue1", selected);
  }

  @Test
  void leastPlayersTreatsOfflineServerAsFullest() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.LEAST_PLAYERS);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    // Only register queue2, so queue1 and queue3 are "offline" (not found)
    TestServer queue2 = plugin.registerServer("queue2");
    queue2.getConnectedPlayers().add(plugin.registerPlayer("P1"));

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2", "queue3"), List.of("main"), List.of(), List.of());

    String selected = selector.selectQueueServer(group);

    // Should select queue2 even though it has players, because others are offline
    assertEquals("queue2", selected);
  }

  @Test
  void randomSelectsFromAllServers() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.RANDOM);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2", "queue3"), List.of("main"), List.of(), List.of());

    Set<String> selected = new HashSet<>();
    // Run enough times to statistically hit all servers
    for (int i = 0; i < 100; i++) {
      selected.add(selector.selectQueueServer(group));
    }

    // All servers should have been selected at least once
    assertTrue(selected.contains("queue1"));
    assertTrue(selected.contains("queue2"));
    assertTrue(selected.contains("queue3"));
  }

  @Test
  void roundRobinHandlesCounterWrapAround() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.ROUND_ROBIN);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2"), List.of("main"), List.of(), List.of());

    // Call many times to ensure it wraps correctly
    for (int i = 0; i < 1000; i++) {
      String selected = selector.selectQueueServer(group);
      assertTrue(selected.equals("queue1") || selected.equals("queue2"));
    }
  }

  @Test
  void selectsCorrectlyAfterStrategyChange() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setQueueLoadBalancing(LoadBalancingStrategy.ROUND_ROBIN);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, Set.of());
    QueueServerSelector selector = new QueueServerSelector(environment);

    TestServer queue1 = plugin.registerServer("queue1");
    plugin.registerServer("queue2"); // Register queue2 but we don't need to reference it
    queue1.getConnectedPlayers().add(plugin.registerPlayer("P1"));

    QueueGroup group = new QueueGroup("multi", List.of("queue1", "queue2"), List.of("main"), List.of(), List.of());

    // Start with round robin
    selector.selectQueueServer(group);

    // Switch to least players
    config.setQueueLoadBalancing(LoadBalancingStrategy.LEAST_PLAYERS);

    String selected = selector.selectQueueServer(group);

    // Should now use least players strategy and select queue2
    assertEquals("queue2", selected);
  }
}
