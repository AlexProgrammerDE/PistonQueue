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
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueTestUtils.TestPlayer;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueTestUtils.TestQueuePlugin;
import net.pistonmaster.pistonqueue.shared.queue.logic.QueueTestUtils.TestServer;
import net.pistonmaster.pistonqueue.shared.queue.logic.ShadowBanService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class QueueMoveProcessorTest {

  @Test
  void movesPlayersInArrivalOrder() {
    MoveContext context = context(5);
    TestPlayer first = context.plugin().registerPlayer("P1");
    enqueue(context, first, "main");
    TestPlayer second = context.plugin().registerPlayer("P2");
    enqueue(context, second, "main");

    context.processor().processQueues();

    assertEquals(List.of("main"), first.getConnections());
    assertEquals(List.of("main"), second.getConnections());
    assertTrue(context.queueType().getQueueMap().isEmpty());
  }

  @Test
  void respectsMaxPlayersPerMove() {
    MoveContext context = context(5);
    context.config().MAX_PLAYERS_PER_MOVE = 1;
    TestPlayer first = context.plugin().registerPlayer("P1");
    enqueue(context, first, "main");
    TestPlayer second = context.plugin().registerPlayer("P2");
    enqueue(context, second, "main");

    context.processor().processQueues();

    assertEquals(List.of("main"), first.getConnections());
    assertFalse(second.getConnections().contains("main"));
    assertEquals(1, context.queueType().getQueueMap().size());
  }

  @Test
  void skipsWhenNoFreeSlots() {
    MoveContext context = context(1);
    context.queueType().getPlayersWithTypeInTarget().set(1);
    TestPlayer player = context.plugin().registerPlayer("Blocked");
    enqueue(context, player, "main");

    context.processor().processQueues();

    assertTrue(player.getConnections().isEmpty());
    assertEquals(1, context.queueType().getQueueMap().size());
  }

  @Test
  void removesStaleQueueEntries() {
    MoveContext context = context(5);
    TestPlayer player = context.plugin().registerPlayer("Ghost");
    enqueue(context, player, "main");
    player.setCurrentServer("limbo");

    context.processor().processQueues();

    assertTrue(context.queueType().getQueueMap().isEmpty());
  }

  @Test
  void recoversPlayersStrandedOnQueueServer() {
    MoveContext context = context(5);
    TestPlayer player = context.plugin().registerPlayer("Returner");
    player.setCurrentServer(context.group().getQueueServer());
    context.queueType().getQueueMap().clear();

    context.processor().processQueues();

    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains(context.config().RECOVERY_MESSAGE)));
    assertTrue(player.getConnections().contains(context.group().getTargetServers().get(0)));
  }

  @Test
  void pausesWhenTargetsOffline() {
    MoveContext context = context(5);
    context.onlineServers().remove(context.group().getTargetServers().get(0));
    TestPlayer player = context.plugin().registerPlayer("Waiting");
    enqueue(context, player, "main");

    context.processor().processQueues();

    assertTrue(player.getConnections().isEmpty());
    assertEquals(1, context.queueType().getQueueMap().size());
  }

  @Test
  void resumesWhenPauseDisabled() {
    MoveContext context = context(5);
    context.config().PAUSE_QUEUE_IF_TARGET_DOWN = false;
    context.onlineServers().remove(context.group().getTargetServers().get(0));
    TestPlayer player = context.plugin().registerPlayer("Bold");
    enqueue(context, player, "main");

    context.processor().processQueues();

    assertEquals(List.of("main"), player.getConnections());
  }

  @Test
  void sendsXpSoundWhenMovingQueue() {
    MoveContext context = context(5);
    for (int i = 0; i < 3; i++) {
      TestPlayer player = context.plugin().registerPlayer("XP" + i);
      enqueue(context, player, "main");
    }

    context.processor().processQueues();

    TestServer queueServer = context.plugin().registerServer(context.group().getQueueServer());
    assertFalse(queueServer.getPluginMessages().isEmpty());
  }

  @Test
  void keepsShadowBannedPlayersInQueue() {
    MoveContext context = context(5);
    TestPlayer player = context.plugin().registerPlayer("Shadowed");
    enqueue(context, player, "main");
    TestPlayer allowed = context.plugin().registerPlayer("Allowed");
    enqueue(context, allowed, "main");
    context.shadowBan(player.getName());

    context.processor().processQueues();

    assertTrue(context.queueType().getQueueMap().containsKey(player.getUniqueId()));
    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains(context.config().SHADOW_BAN_MESSAGE)));
    context.unShadowBan(player.getName());
  }

  @Test
  void recordsDurationForKnownPosition() {
    MoveContext context = context(5);
    TestPlayer player = context.plugin().registerPlayer("Timer");
    enqueue(context, player, "main");
    context.queueType().getPositionCache().put(player.getUniqueId(), new java.util.HashMap<>(java.util.Map.of(1, Instant.now().minusSeconds(5))));

    context.processor().processQueues();

    assertFalse(context.queueType().getDurationFromPosition().isEmpty());
  }

  @Test
  void handlesConcurrentProcessingWithoutDuplicates() throws Exception {
    MoveContext context = context(50);
    List<TestPlayer> players = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      TestPlayer player = context.plugin().registerPlayer("C" + i);
      enqueue(context, player, "main");
      players.add(player);
    }

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<Void> first = CompletableFuture.runAsync(() -> {
      await(latch);
      context.processor().processQueues();
    }, executor);
    CompletableFuture<Void> second = CompletableFuture.runAsync(() -> {
      await(latch);
      context.processor().processQueues();
    }, executor);
    latch.countDown();
    CompletableFuture.allOf(first, second).get(5, TimeUnit.SECONDS);
    executor.shutdownNow();

    long totalConnections = players.stream().mapToLong(p -> p.getConnections().size()).sum();
    assertEquals(players.size(), totalConnections);
    assertTrue(context.queueType().getQueueMap().isEmpty());
  }

  @Test
  void movesAfterTargetsComeBackOnline() {
    MoveContext context = context(5);
    String target = context.group().getTargetServers().get(0);
    context.onlineServers().remove(target);
    TestPlayer player = context.plugin().registerPlayer("Flip");
    enqueue(context, player, target);

    context.processor().processQueues();
    assertTrue(player.getConnections().isEmpty());

    context.onlineServers().add(target);
    context.processor().processQueues();

    assertEquals(List.of(target), player.getConnections());
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private void enqueue(MoveContext context, TestPlayer player, String target) {
    player.setCurrentServer(context.group().getQueueServer());
    context.queueType().getQueueMap().put(player.getUniqueId(), new QueueType.QueuedPlayer(target, QueueReason.SERVER_FULL));
  }

  private MoveContext context(int slots) {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(slots);
    TestQueuePlugin plugin = new TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> online = QueueTestUtils.onlineServers(group.getQueueServer(), group.getTargetServers().get(0));
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, online);
    QueueAvailabilityCalculator calculator = new QueueAvailabilityCalculator();
    QueueCleaner cleaner = new QueueCleaner(environment);
    QueueRecoveryHandler recovery = new QueueRecoveryHandler(environment);
    Set<String> shadowBannedNames = ConcurrentHashMap.newKeySet();
    ShadowBanService shadowBanService = shadowBannedNames::contains;
    QueueConnector connector = new QueueConnector(environment, calculator, shadowBanService);
    QueueMoveProcessor processor = new QueueMoveProcessor(environment, cleaner, recovery, connector);
    return new MoveContext(config, plugin, environment, processor, group, config.QUEUE_TYPES[0], online, shadowBannedNames);
  }

  private record MoveContext(
    Config config,
    TestQueuePlugin plugin,
    QueueEnvironment environment,
    QueueMoveProcessor processor,
    QueueGroup group,
    QueueType queueType,
    Set<String> onlineServers,
    Set<String> shadowBannedNames
  ) {
    void shadowBan(String name) {
      shadowBannedNames.add(name);
    }

    void unShadowBan(String name) {
      shadowBannedNames.remove(name);
    }
  }
}
