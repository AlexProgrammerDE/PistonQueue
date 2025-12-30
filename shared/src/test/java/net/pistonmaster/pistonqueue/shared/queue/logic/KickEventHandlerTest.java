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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class KickEventHandlerTest {

  @Test
  void redirectsToQueueWhenKickedFromDownTargetServer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setTargetServer("target");
    config.setIfTargetDownSendToQueue(true);
    config.setDownWordList(List.of("server", "down", "offline"));
    config.setIfTargetDownSendToQueueMessage("Server is down, redirecting to queue");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Server is currently down");

    handler.handleKick(event);

    assertTrue(group.hasQueueServer(event.getCancelServer().orElseThrow()));
    assertTrue(player.getMessages().stream().anyMatch(msg -> msg.contains("Server is down, redirecting to queue")));
    assertTrue(QueueTestUtils.defaultQueueType(config).getQueueMap().containsKey(player.getUniqueId()));
    assertEquals("target", QueueTestUtils.defaultQueueType(config).getQueueMap().get(player.getUniqueId()).targetServer());
  }

  @Test
  void doesNotRedirectWhenQueueRedirectionDisabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setTargetServer("target");
    config.setIfTargetDownSendToQueue(false);
    config.setDownWordList(List.of("down"));

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Server is down");

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(QueueTestUtils.defaultQueueType(config).getQueueMap().isEmpty());
  }

  @Test
  void doesNotRedirectWhenKickedFromNonTargetServer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setTargetServer("target");
    config.setIfTargetDownSendToQueue(true);
    config.setDownWordList(List.of("down"));

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "lobby", "Server is down");

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(QueueTestUtils.defaultQueueType(config).getQueueMap().isEmpty());
  }

  @Test
  void doesNotRedirectWhenKickReasonDoesNotMatch() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setTargetServer("target");
    config.setIfTargetDownSendToQueue(true);
    config.setDownWordList(List.of("down"));

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "You were kicked by an admin");

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(QueueTestUtils.defaultQueueType(config).getQueueMap().isEmpty());
  }

  @Test
  void setsCustomKickMessageWhenEnabledAndWillDisconnect() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setEnableKickMessage(true);
    config.setKickMessage("Custom kick message");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Some reason");
    event.setWillDisconnect(true);

    handler.handleKick(event);

    assertEquals("Custom kick message", event.getKickMessage().orElseThrow());
  }

  @Test
  void doesNotSetKickMessageWhenDisabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setEnableKickMessage(false);
    config.setKickMessage("Custom kick message");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Some reason");
    event.setWillDisconnect(true);

    handler.handleKick(event);

    assertTrue(event.getKickMessage().isEmpty());
  }

  @Test
  void doesNotSetKickMessageWhenNotDisconnecting() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setEnableKickMessage(true);
    config.setKickMessage("Custom kick message");

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "Some reason");
    event.setWillDisconnect(false);

    handler.handleKick(event);

    assertTrue(event.getKickMessage().isEmpty());
  }

  @Test
  void handlesKickWithoutReason() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setTargetServer("target");
    config.setIfTargetDownSendToQueue(true);
    config.setDownWordList(List.of("down"));

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", null); // No kick reason

    handler.handleKick(event);

    assertTrue(event.getCancelServer().isEmpty());
    assertTrue(QueueTestUtils.defaultQueueType(config).getQueueMap().isEmpty());
  }

  @Test
  void caseInsensitiveKickReasonMatching() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setTargetServer("target");
    config.setIfTargetDownSendToQueue(true);
    config.setDownWordList(List.of("server", "DOWN"));

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueGroup group = QueueTestUtils.defaultGroup(config);
    Set<String> onlineServers = QueueTestUtils.onlineServers("queue", "target");
    QueueEnvironment environment = new QueueEnvironment(plugin, plugin::getConfiguration,  () -> onlineServers);
    QueueServerSelector selector = new QueueServerSelector(environment);
    KickEventHandler handler = new KickEventHandler(config, environment, selector);

    QueueTestUtils.TestPlayer player = plugin.registerPlayer("KickedPlayer");
    QueueTestUtils.TestKickEvent event = QueueTestUtils.kickEvent(player, "target", "SERVER IS CURRENTLY down");

    handler.handleKick(event);

    assertTrue(group.hasQueueServer(event.getCancelServer().orElseThrow()));
    assertTrue(QueueTestUtils.defaultQueueType(config).getQueueMap().containsKey(player.getUniqueId()));
  }
}
