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
package net.pistonmaster.pistonqueue.shared.plugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.val;
import net.pistonmaster.pistonqueue.shared.chat.MessageType;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.config.ConfigOutdatedException;
import net.pistonmaster.pistonqueue.shared.queue.QueueListenerShared;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.utils.SharedChatUtils;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public interface PistonQueuePlugin {
  Optional<PlayerWrapper> getPlayer(UUID uuid);

  List<PlayerWrapper> getPlayers();

  Optional<ServerInfoWrapper> getServer(String name);

  void schedule(Runnable runnable, long delay, long period, TimeUnit unit);

  void info(String message);

  void warning(String message);

  void error(String message);

  List<String> getAuthors();

  String getVersion();

  Path getDataDirectory();

  default void scheduleTasks(QueueListenerShared queueListener) {
    // Sends the position message and updates tab on an interval in chat
    schedule(() -> {
      if (queueListener.getOnlineServers().contains(Config.TARGET_SERVER)) {
        for (QueueType type : Config.QUEUE_TYPES) {
          if (Config.POSITION_MESSAGE_CHAT) {
            sendMessage(type, MessageType.CHAT);
          }
          if (Config.POSITION_MESSAGE_HOT_BAR) {
            sendMessage(type, MessageType.ACTION_BAR);
          }
        }
      } else if (Config.PAUSE_QUEUE_IF_TARGET_DOWN) {
        for (QueueType type : Config.QUEUE_TYPES) {
          type.getQueueMap().forEach((id, queuedPlayer) ->
            getPlayer(id).ifPresent(value -> value.sendMessage(Config.PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE)));
        }
      }
    }, Config.POSITION_MESSAGE_DELAY, Config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

    // Updates the tab
    schedule(() -> {
      if (!Config.POSITION_PLAYER_LIST) {
        return;
      }

      for (QueueType type : Config.QUEUE_TYPES) {
        updateTab(type);
      }
    }, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

    // Send plugin message
    schedule(this::sendCustomData, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

    // Moves the queue when someone logs off the target server on an interval set in the config.yml
    schedule(queueListener::moveQueue, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

    // Checks the status of all the servers
    schedule(() -> {
      List<String> servers = new ArrayList<>(Config.KICK_WHEN_DOWN_SERVERS);
      CountDownLatch latch = new CountDownLatch(servers.size());
      for (String server : servers) {
        CompletableFuture.runAsync(() -> {
          try {
            Optional<ServerInfoWrapper> serverInfoWrapper = getServer(server);

            if (serverInfoWrapper.isPresent()) {
              if (serverInfoWrapper.get().isOnline()) {
                queueListener.getOnlineServers().add(server);
              } else {
                warning(String.format("Server %s is down!!!", server));
                queueListener.getOnlineServers().remove(server);
              }
            } else {
              warning(String.format("Server \"%s\" not set up!!! Check out: https://github.com/AlexProgrammerDE/PistonQueue/wiki/FAQ#server-not-set-up", server));
            }
          } finally {
            latch.countDown();
          }
        });
      }
      try {
        latch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }, 500, Config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);
  }

  default void sendMessage(QueueType queue, MessageType type) {
    AtomicInteger position = new AtomicInteger();
    for (Map.Entry<UUID, QueueType.QueuedPlayer> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
      Optional<PlayerWrapper> player = getPlayer(entry.getKey());
      if (player.isEmpty()) {
        continue;
      }

      String chatMessage = Config.QUEUE_POSITION
        .replace("%position%", String.valueOf(position.incrementAndGet()))
        .replace("%total%", String.valueOf(queue.getQueueMap().size()));

      player.get().sendMessage(type, chatMessage);
    }
  }

  default void updateTab(QueueType queue) {
    AtomicInteger position = new AtomicInteger();

    for (Map.Entry<UUID, QueueType.QueuedPlayer> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
      getPlayer(entry.getKey()).ifPresent(player -> {
        int incrementedPosition = position.incrementAndGet();

        player.sendPlayerList(
          queue.getHeader().stream().map(str -> replacePosition(str, incrementedPosition, queue)).collect(Collectors.toList()),
          queue.getFooter().stream().map(str -> replacePosition(str, incrementedPosition, queue)).collect(Collectors.toList()));
      });
    }
  }

  default String replacePosition(String text, int position, QueueType type) {
    if (type.getDurationFromPosition().containsKey(position)) {
      Duration duration = type.getDurationFromPosition().get(position);

      return SharedChatUtils.formatDuration(text, duration, position);
    } else {
      Map.Entry<Integer, Duration> biggestEntry = null;

      for (Map.Entry<Integer, Duration> entry : type.getDurationFromPosition().entrySet()) {
        if (biggestEntry == null || entry.getKey() > biggestEntry.getKey()) {
          biggestEntry = entry;
        }
      }

      Duration predictedDuration = biggestEntry == null ?
        Duration.of(position, ChronoUnit.MINUTES) :
        biggestEntry.getValue().plus(position - biggestEntry.getKey(), ChronoUnit.MINUTES);

      return SharedChatUtils.formatDuration(text, predictedDuration, position);
    }
  }

  default void initializeReservationSlots() {
    schedule(() -> {
      Optional<ServerInfoWrapper> targetServer = getServer(Config.TARGET_SERVER);
      if (targetServer.isEmpty())
        return;

      Map<QueueType, AtomicInteger> map = new HashMap<>();

      for (PlayerWrapper player : targetServer.get().getConnectedPlayers()) {
        QueueType playerType = QueueType.getQueueType(player);

        map.compute(playerType, (queueType, integer) -> {
          if (integer == null) {
            return new AtomicInteger(1);
          } else {
            integer.incrementAndGet();
            return integer;
          }
        });
      }

      map.forEach((type, count) -> type.getPlayersWithTypeInTarget().set(count.get()));
    }, 0, 1, TimeUnit.SECONDS);
  }

  default void sendCustomData() {
    List<PlayerWrapper> networkPlayers = getPlayers();

    if (networkPlayers == null || networkPlayers.isEmpty()) {
      return;
    }

    ByteArrayDataOutput outOnlineQueue = ByteStreams.newDataOutput();

    outOnlineQueue.writeUTF("onlineQueue");
    outOnlineQueue.writeInt(Config.QUEUE_TYPES.length);
    for (QueueType queueType : Config.QUEUE_TYPES) {
      outOnlineQueue.writeUTF(queueType.getName().toLowerCase(Locale.ROOT));
      outOnlineQueue.writeInt(queueType.getQueueMap().size());
    }

    ByteArrayDataOutput outOnlineTarget = ByteStreams.newDataOutput();

    outOnlineTarget.writeUTF("onlineTarget");
    outOnlineTarget.writeInt(Config.QUEUE_TYPES.length);
    for (QueueType queueType : Config.QUEUE_TYPES) {
      outOnlineTarget.writeUTF(queueType.getName().toLowerCase(Locale.ROOT));
      outOnlineTarget.writeInt(queueType.getPlayersWithTypeInTarget().get());
    }

    Set<String> servers = new HashSet<>();
    networkPlayers.forEach(player -> player.getCurrentServer().ifPresent(servers::add));

    for (String server : servers) {
      getServer(server).ifPresent(serverInfoWrapper -> {
        serverInfoWrapper.sendPluginMessage("piston:queue", outOnlineQueue.toByteArray());
        serverInfoWrapper.sendPluginMessage("piston:queue", outOnlineTarget.toByteArray());
      });
    }
  }

  default void processConfig(Path dataDirectory) {
    try {
      if (!Files.exists(dataDirectory)) {
        Files.createDirectories(dataDirectory);
      }

      Path file = dataDirectory.resolve("config.yml");

      if (!Files.exists(file)) {
        Files.copy(Objects.requireNonNull(PistonQueuePlugin.class.getClassLoader().getResourceAsStream("proxy_config.yml")), file);
      }

      loadConfig(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  default void loadConfig(Path file) throws IOException {
    ConfigurationNode config = YamlConfigurationLoader.builder().path(file).build().load();

    Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
      try {
        it.setAccessible(true);

        String fieldName = it.getName();
        if (List.class.isAssignableFrom(it.getType())) {
          it.set(Config.class, config.node(fieldName).getList(String.class));
        } else if (fieldName.equals("QUEUE_TYPES")) {
          if (it.get(Config.class) == null) { // We will never replace on reload
            val queueTypes = config.node("QUEUE_TYPES").childrenMap();
            val array = new QueueType[queueTypes.size()];
            int i = 0;
            for (val entry : queueTypes.entrySet()) {
              Object key = entry.getKey();
              ConfigurationNode typeData = entry.getValue();
              QueueType queueType = new QueueType(
                key.toString(),
                typeData.node("ORDER").getInt(),
                typeData.node("PERMISSION").getString(),
                typeData.node("SLOTS").getInt(),
                typeData.node("HEADER").getList(String.class),
                typeData.node("FOOTER").getList(String.class));

              array[i] = queueType;
              i++;
            }
            Arrays.sort(array, Comparator.comparingInt(QueueType::getOrder));
            it.set(Config.class, array);
          } else { // Modify existing
            QueueType[] queueType = (QueueType[]) it.get(Config.class);
            for (QueueType type : queueType) {
              ConfigurationNode typeData = config.node("QUEUE_TYPES").node(type.getName());
              type.setOrder(typeData.node("ORDER").getInt());
              type.setPermission(typeData.node("PERMISSION").getString());
              type.setReservedSlots(typeData.node("SLOTS").getInt());
              type.setHeader(typeData.node("HEADER").getList(String.class));
              type.setFooter(typeData.node("FOOTER").getList(String.class));
            }
          }
        } else {
          it.set(Config.class, config.node(fieldName).get(it.getType()));
        }
      } catch (SecurityException | IllegalAccessException | SerializationException e) {
        e.printStackTrace();
      } catch (IllegalArgumentException e) {
        String[] text = e.getMessage().split(" ");
        String value = "";

        for (String str : text) {
          if (str.toLowerCase(Locale.ROOT).startsWith(Config.class.getPackage().getName().toLowerCase(Locale.ROOT))) {
            value = str;
          }
        }

        String[] packageSplit = value.split("\\.");

        new ConfigOutdatedException(packageSplit[packageSplit.length - 1]).printStackTrace();
      }
    });
    int i = 0;
    for (String text : Config.KICK_WHEN_DOWN_SERVERS) {
      Config.KICK_WHEN_DOWN_SERVERS.set(i, text
        .replace("%TARGET_SERVER%", Config.TARGET_SERVER)
        .replace("%QUEUE_SERVER%", Config.QUEUE_SERVER)
        .replace("%SOURCE_SERVER%", Config.SOURCE_SERVER));
      i++;
    }
    if (!Config.KICK_WHEN_DOWN_SERVERS.contains(Config.TARGET_SERVER)) {
      Config.KICK_WHEN_DOWN_SERVERS.add(Config.TARGET_SERVER);
    }
  }
}
