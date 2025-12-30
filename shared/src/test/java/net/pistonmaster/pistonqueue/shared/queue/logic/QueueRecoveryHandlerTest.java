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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueueRecoveryHandlerTest {

  private static Set<String> serversFromGroup(QueueGroup group) {
    List<String> servers = new ArrayList<>();
    servers.addAll(group.queueServers());
    servers.addAll(group.targetServers());
    return QueueTestUtils.onlineServers(servers.toArray(String[]::new));
  }

  @Test
  void recoversPlayerOnQueueServer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = serversFromGroup(group);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueRecoveryHandler recoveryHandler = new QueueRecoveryHandler(environment);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Recover");
    player.setCurrentServer(group.queueServers().getFirst());

    recoveryHandler.recoverPlayer(player);

    assertTrue(type.getQueueMap().containsKey(player.getUniqueId()));
    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains(config.recoveryMessage())));
  }

  @Test
  void doesNotRecoverPlayerNotOnQueueServer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueRecoveryHandler recoveryHandler = new QueueRecoveryHandler(environment);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("NotRecover");
    player.setCurrentServer("otherServer");

    recoveryHandler.recoverPlayer(player);

    assertTrue(type.getQueueMap().isEmpty());
    assertTrue(player.getMessages().isEmpty());
  }

  @Test
  void doesNotRecoverPlayerAlreadyInActiveTransfer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers(group.queueServers().toArray(String[]::new));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueRecoveryHandler recoveryHandler = new QueueRecoveryHandler(environment);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Transferring");
    player.setCurrentServer(group.queueServers().getFirst());
    type.getActiveTransfers().add(player.getUniqueId());

    recoveryHandler.recoverPlayer(player);

    assertTrue(type.getQueueMap().isEmpty());
  }

  @Test
  void doesNotDuplicateRecoveryEntries() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = serversFromGroup(group);
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueRecoveryHandler recoveryHandler = new QueueRecoveryHandler(environment);

    QueueType type = QueueTestUtils.defaultQueueType(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("Duplicate");
    player.setCurrentServer(group.queueServers().getFirst());
    type.getQueueMap().put(player.getUniqueId(), new QueueType.QueuedPlayer("target", QueueReason.RECOVERY));

    recoveryHandler.recoverPlayer(player);

    assertEquals(1, type.getQueueMap().size());
    // Should not send duplicate messages - player is already queued so no message sent
    long recoveryMessageCount = player.getMessages().stream()
      .filter(msg -> msg.contains(config.recoveryMessage()))
      .count();
    assertEquals(0, recoveryMessageCount);
  }
}
