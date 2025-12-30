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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueueEntryFactoryTest {

  @Test
  void enqueuesPlayerWithCorrectTarget() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    QueueEntryFactory entryFactory = new QueueEntryFactory(environment, selector);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Enqueue");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, "customTarget");

    entryFactory.enqueue(player, group, type, event, true, config);

    assertTrue(group.hasQueueServer(event.getTarget().orElseThrow()));
    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
    assertEquals("customTarget", type.getQueueMap().get(player.getUniqueId()).targetServer());
  }

  @Test
  void forcesDefaultTargetWhenConfigured() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setForceTargetServer(true);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    String[] servers = new String[group.queueServers().size() + group.targetServers().size()];
    int i = 0;
    for (String s : group.queueServers()) servers[i++] = s;
    for (String s : group.targetServers()) servers[i++] = s;
    Set<String> onlineServers = QueueTestUtils.onlineServers(servers);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    QueueEntryFactory entryFactory = new QueueEntryFactory(environment, selector);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Force");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, "customTarget");

    entryFactory.enqueue(player, group, type, event, true, config);

    assertEquals(group.targetServers().getFirst(), type.getQueueMap().get(player.getUniqueId()).targetServer());
  }

  @Test
  void sendsFullMessageOnlyOnce() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    QueueEntryFactory entryFactory = new QueueEntryFactory(environment, selector);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Full");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, "target");

    entryFactory.enqueue(player, group, type, event, true, config);
    entryFactory.enqueue(player, group, type, event, true, config); // Try to enqueue again

    long fullMessageCount = player.getMessages().stream()
      .filter(msg -> msg.contains(config.serverIsFullMessage()))
      .count();
    assertEquals(1, fullMessageCount);
  }

  @Test
  void setsPlayerListHeaderAndFooter() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    QueueEntryFactory entryFactory = new QueueEntryFactory(environment, selector);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    type.setHeader(List.of("Header Line 1", "Header Line 2"));
    type.setFooter(List.of("Footer Line 1"));
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("List");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, "target");

    entryFactory.enqueue(player, group, type, event, false, config);

    assertEquals(List.of("Header Line 1", "Header Line 2"), player.getPlayerListHeader());
    assertEquals(List.of("Footer Line 1"), player.getPlayerListFooter());
  }

  @Test
  void doesNotSendFullMessageWhenNotServerFull() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    QueueEntryFactory entryFactory = new QueueEntryFactory(environment, selector);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("NotFull");
    QueueTestUtils.TestPreConnectEvent event = QueueTestUtils.preConnectEvent(player, "target");

    entryFactory.enqueue(player, group, type, event, false, config);

    assertTrue(player.getMessages().stream()
      .noneMatch(msg -> msg.contains(config.serverIsFullMessage())));
  }
}
