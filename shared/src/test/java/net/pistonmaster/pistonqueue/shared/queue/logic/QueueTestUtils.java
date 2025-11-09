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

import net.pistonmaster.pistonqueue.shared.chat.MessageType;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class QueueTestUtils {
  private static volatile boolean storageInitialized;

  private QueueTestUtils() {
  }

  static void ensureStorageToolInitialized() {
    if (storageInitialized) {
      return;
    }
    synchronized (QueueTestUtils.class) {
      if (storageInitialized) {
        return;
      }
      try {
        Path dir = Files.createTempDirectory("pq-storage-test");
        StorageTool.setupTool(dir);
        storageInitialized = true;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  static Config createConfigWithSingleQueueType(int slots) {
    Config config = new Config();
    config.copyFrom(config); // Initializes defaults and queue groups
    QueueType queueType = new QueueType(
      "DEFAULT",
      1,
      "default",
      slots,
      new ArrayList<>(),
      new ArrayList<>());
    config.QUEUE_TYPES = new QueueType[]{queueType};
    rebuildQueueGroups(config);
    return config;
  }

  static QueueGroup defaultGroup(Config config) {
    return config.getQueueGroups().iterator().next();
  }

  static void rebuildQueueGroups(Config config) {
    try {
      Method rebuild = Config.class.getDeclaredMethod("rebuildQueueGroups");
      rebuild.setAccessible(true);
      rebuild.invoke(config);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  static TestPreConnectEvent preConnectEvent(PlayerWrapper player, String target) {
    return new TestPreConnectEvent(player, target);
  }

  static Set<String> onlineServers(String... names) {
    Set<String> servers = Collections.synchronizedSet(new HashSet<>());
    Collections.addAll(servers, names);
    return servers;
  }

  static final class TestQueuePlugin implements PistonQueuePlugin {
    private final Map<UUID, TestPlayer> players = new HashMap<>();
    private final Map<String, TestServer> servers = new HashMap<>();
    private final Config config;
    private final Path dataDirectory;

    TestQueuePlugin(Config config) {
      this.config = Objects.requireNonNull(config, "config");
      try {
        this.dataDirectory = Files.createTempDirectory("pq-plugin-test");
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      // Pre-register queue and target servers used by tests
      registerServer(config.QUEUE_SERVER);
      registerServer(config.TARGET_SERVER);
    }

    TestServer registerServer(String name) {
      return servers.computeIfAbsent(name, TestServer::new);
    }

    TestPlayer registerPlayer(String name) {
      TestPlayer player = new TestPlayer(name);
      players.put(player.getUniqueId(), player);
      return player;
    }

    @Override
    public Optional<PlayerWrapper> getPlayer(UUID uuid) {
      return Optional.ofNullable(players.get(uuid));
    }

    @Override
    public List<PlayerWrapper> getPlayers() {
      return new ArrayList<>(players.values());
    }

    @Override
    public Optional<ServerInfoWrapper> getServer(String name) {
      return Optional.ofNullable(servers.get(name));
    }

    @Override
    public void schedule(Runnable runnable, long delay, long period, java.util.concurrent.TimeUnit unit) {
      // Not needed in tests
    }

    @Override
    public void info(String message) {
    }

    @Override
    public void warning(String message) {
    }

    @Override
    public void error(String message) {
    }

    @Override
    public List<String> getAuthors() {
      return List.of("test");
    }

    @Override
    public String getVersion() {
      return "test";
    }

    @Override
    public Path getDataDirectory() {
      return dataDirectory;
    }

    @Override
    public Config getConfiguration() {
      return config;
    }
  }

  static final class TestPlayer implements PlayerWrapper {
    private final UUID uniqueId = UUID.randomUUID();
    private final String name;
    private final Set<String> permissions = new HashSet<>();
    private Optional<String> currentServer = Optional.empty();
    private final List<String> messages = new ArrayList<>();
    private final List<String> playerListHeader = new ArrayList<>();
    private final List<String> playerListFooter = new ArrayList<>();
    private final List<String> connections = new ArrayList<>();
    private boolean disconnected;
    private String disconnectMessage;

    TestPlayer(String name) {
      this.name = name;
    }

    @Override
    public void connect(String server) {
      connections.add(server);
      currentServer = Optional.ofNullable(server);
    }

    @Override
    public Optional<String> getCurrentServer() {
      return currentServer;
    }

    public void setCurrentServer(String server) {
      currentServer = Optional.ofNullable(server);
    }

    @Override
    public void sendMessage(MessageType type, String message) {
      messages.add(type.name() + ":" + message);
    }

    @Override
    public void sendPlayerList(List<String> header, List<String> footer) {
      playerListHeader.clear();
      playerListHeader.addAll(header);
      playerListFooter.clear();
      playerListFooter.addAll(footer);
    }

    @Override
    public void resetPlayerList() {
      playerListHeader.clear();
      playerListFooter.clear();
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public UUID getUniqueId() {
      return uniqueId;
    }

    @Override
    public void disconnect(String message) {
      disconnected = true;
      disconnectMessage = message;
    }

    @Override
    public boolean hasPermission(String node) {
      return permissions.contains(node);
    }

    public void grantPermission(String node) {
      permissions.add(node);
    }

    public List<String> getMessages() {
      return messages;
    }

    public List<String> getConnections() {
      return connections;
    }

    public boolean isDisconnected() {
      return disconnected;
    }

    public String getDisconnectMessage() {
      return disconnectMessage;
    }

    public List<String> getPlayerListHeader() {
      return playerListHeader;
    }

    public List<String> getPlayerListFooter() {
      return playerListFooter;
    }
  }

  static final class TestServer implements ServerInfoWrapper {
    private final String name;
    private final List<PlayerWrapper> connectedPlayers = new ArrayList<>();
    private final List<byte[]> pluginMessages = new ArrayList<>();
    private boolean online = true;

    TestServer(String name) {
      this.name = name;
    }

    @Override
    public List<PlayerWrapper> getConnectedPlayers() {
      return connectedPlayers;
    }

    @Override
    public boolean isOnline() {
      return online;
    }

    public void setOnline(boolean online) {
      this.online = online;
    }

    @Override
    public void sendPluginMessage(String channel, byte[] data) {
      pluginMessages.add(data);
    }

    public List<byte[]> getPluginMessages() {
      return pluginMessages;
    }

    public String getName() {
      return name;
    }
  }

  static final class TestPreConnectEvent implements PQServerPreConnectEvent {
    private final PlayerWrapper player;
    private String target;

    TestPreConnectEvent(PlayerWrapper player, String target) {
      this.player = player;
      this.target = target;
    }

    @Override
    public PlayerWrapper getPlayer() {
      return player;
    }

    @Override
    public Optional<String> getTarget() {
      return Optional.ofNullable(target);
    }

    @Override
    public void setTarget(String server) {
      this.target = server;
    }
  }
}
