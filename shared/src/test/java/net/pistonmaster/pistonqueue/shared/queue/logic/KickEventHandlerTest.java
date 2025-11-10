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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KickEventHandlerTest {

  @Test
  void redirectsToQueueWhenKickedFromDownTargetServer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.TARGET_SERVER = "target"; // Set the target server
    QueueTestUtils.rebuildQueueGroups(config); // Rebuild groups after changing target server
    config.IF_TARGET_DOWN_SEND_TO_QUEUE = true;
    config.DOWN_WORD_LIST = List.of("server", "down", "offline");
    config.IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE = "Server is down, redirecting to queue";

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Server is currently down");

    handler.handleKick(event);

    assertEquals("queue", event.getCancelServer().orElseThrow());
    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains("Server is down, redirecting to queue")));
    assertTrue(config.QUEUE_TYPES[0].getQueueMap().containsKey(player.getUniqueId()));
    assertEquals("target", config.QUEUE_TYPES[0].getQueueMap().get(player.getUniqueId()).targetServer());
  }

  @Test
  void doesNotRedirectWhenQueueRedirectionDisabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.TARGET_SERVER = "target"; // Set the target server
    config.IF_TARGET_DOWN_SEND_TO_QUEUE = false;
    config.DOWN_WORD_LIST = List.of("down");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Server is down");

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(config.QUEUE_TYPES[0].getQueueMap().isEmpty());
  }

  @Test
  void doesNotRedirectWhenKickedFromNonTargetServer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.TARGET_SERVER = "target"; // Set the target server
    config.IF_TARGET_DOWN_SEND_TO_QUEUE = true;
    config.DOWN_WORD_LIST = List.of("down");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "lobby", "Server is down");

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(config.QUEUE_TYPES[0].getQueueMap().isEmpty());
  }

  @Test
  void doesNotRedirectWhenKickReasonDoesNotMatch() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.TARGET_SERVER = "target"; // Set the target server
    config.IF_TARGET_DOWN_SEND_TO_QUEUE = true;
    config.DOWN_WORD_LIST = List.of("down");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "You were kicked by an admin");

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(config.QUEUE_TYPES[0].getQueueMap().isEmpty());
  }

  @Test
  void setsCustomKickMessageWhenEnabledAndWillDisconnect() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.ENABLE_KICK_MESSAGE = true;
    config.KICK_MESSAGE = "Custom kick message";

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Some reason");
    event.setWillDisconnect(true);

    handler.handleKick(event);

    assertEquals("Custom kick message", event.getKickMessage().orElseThrow());
  }

  @Test
  void doesNotSetKickMessageWhenDisabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.ENABLE_KICK_MESSAGE = false;
    config.KICK_MESSAGE = "Custom kick message";

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Some reason");
    event.setWillDisconnect(true);

    handler.handleKick(event);

    assertTrue(event.getKickMessage().isEmpty());
  }

  @Test
  void doesNotSetKickMessageWhenNotDisconnecting() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.ENABLE_KICK_MESSAGE = true;
    config.KICK_MESSAGE = "Custom kick message";

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Some reason");
    event.setWillDisconnect(false);

    handler.handleKick(event);

    assertTrue(event.getKickMessage().isEmpty());
  }

  @Test
  void handlesKickWithoutReason() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.TARGET_SERVER = "target"; // Set the target server
    config.IF_TARGET_DOWN_SEND_TO_QUEUE = true;
    config.DOWN_WORD_LIST = List.of("down");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", null); // No kick reason

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(config.QUEUE_TYPES[0].getQueueMap().isEmpty());
  }

  @Test
  void caseInsensitiveKickReasonMatching() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.TARGET_SERVER = "target"; // Set the target server
    QueueTestUtils.rebuildQueueGroups(config); // Rebuild groups after changing target server
    config.IF_TARGET_DOWN_SEND_TO_QUEUE = true;
    config.DOWN_WORD_LIST = List.of("server", "DOWN");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration, onlineServers);
    KickEventHandler handler = new KickEventHandler(config, environment);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "SERVER IS CURRENTLY down");

    handler.handleKick(event);

    assertEquals("queue", event.getCancelServer().orElseThrow());
    assertTrue(config.QUEUE_TYPES[0].getQueueMap().containsKey(player.getUniqueId()));
  }
}
