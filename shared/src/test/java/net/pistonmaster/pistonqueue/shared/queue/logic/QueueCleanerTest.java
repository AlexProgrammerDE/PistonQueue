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
import net.pistonmaster.pistonqueue.shared.queue.QueueType.QueueReason;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueueCleanerTest {

  @Test
  void removesStaleEntriesWhenPlayerDisconnected() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueCleaner cleaner = new QueueCleaner(environment);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Stale");
    type.getQueueMap().put(player.getUniqueId(), new QueueType.QueuedPlayer("target", QueueReason.SERVER_FULL));
    // Player is not on the queue server anymore

    cleaner.cleanGroup(group);

    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void keepsActiveQueueEntries() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueCleaner cleaner = new QueueCleaner(environment);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Active");
    player.setCurrentServer(group.queueServers().getFirst());
    type.getQueueMap().put(player.getUniqueId(), new QueueType.QueuedPlayer("target", QueueReason.SERVER_FULL));

    cleaner.cleanGroup(group);

    assertEquals(1, type.getQueueMap().size());
    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
  }

  @Test
  void handlesEmptyQueue() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueCleaner cleaner = new QueueCleaner(environment);

    cleaner.cleanGroup(group);

    // Should not throw any exceptions
  }

  @Test
  void removesMultipleStaleEntries() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueCleaner cleaner = new QueueCleaner(environment);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player1 = plugin.registerPlayer("Stale1");
    QueueTestUtils.TestPlayer player2 = plugin.registerPlayer("Stale2");
    type.getQueueMap().put(player1.getUniqueId(), new QueueType.QueuedPlayer("target", QueueReason.SERVER_FULL));
    type.getQueueMap().put(player2.getUniqueId(), new QueueType.QueuedPlayer("target", QueueReason.SERVER_FULL));
    // Both players are not on the queue server

    cleaner.cleanGroup(group);

    assertTrue(type.getQueueMap().isEmpty());
  }
}
