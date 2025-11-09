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
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurations;
import net.pistonmaster.pistonqueue.shared.chat.MessageType;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.QueueListenerShared;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.utils.SharedChatUtils;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;

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

  Config getConfiguration();

  default void scheduleTasks(QueueListenerShared queueListener) {
    Config config = getConfiguration();
    // Sends the position message and updates tab on an interval in chat
    schedule(() -> {
      if (queueListener.getOnlineServers().contains(config.TARGET_SERVER)) {
        for (QueueType type : config.QUEUE_TYPES) {
          if (config.POSITION_MESSAGE_CHAT) {
            sendMessage(type, MessageType.CHAT);
          }
          if (config.POSITION_MESSAGE_HOT_BAR) {
            sendMessage(type, MessageType.ACTION_BAR);
          }
        }
      } else if (config.PAUSE_QUEUE_IF_TARGET_DOWN) {
        for (QueueType type : config.QUEUE_TYPES) {
          type.getQueueMap().forEach((id, queuedPlayer) ->
            getPlayer(id).ifPresent(value -> value.sendMessage(config.PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE)));
        }
      }
    }, config.POSITION_MESSAGE_DELAY, config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

    // Updates the tab
    schedule(() -> {
      if (!config.POSITION_PLAYER_LIST) {
        return;
      }

      for (QueueType type : config.QUEUE_TYPES) {
        updateTab(type);
      }
    }, config.QUEUE_MOVE_DELAY, config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

    // Send plugin message
    schedule(this::sendCustomData, config.QUEUE_MOVE_DELAY, config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

    // Moves the queue when someone logs off the target server on an interval set in the config.yml
    schedule(queueListener::moveQueue, config.QUEUE_MOVE_DELAY, config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

    // Checks the status of all the servers
    schedule(() -> {
      List<String> servers = new ArrayList<>(config.KICK_WHEN_DOWN_SERVERS);
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
    }, 500, config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);
  }

  default void sendMessage(QueueType queue, MessageType type) {
    Config config = getConfiguration();
    AtomicInteger position = new AtomicInteger();
    for (Map.Entry<UUID, QueueType.QueuedPlayer> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
      Optional<PlayerWrapper> player = getPlayer(entry.getKey());
      if (player.isEmpty()) {
        continue;
      }

      String chatMessage = config.QUEUE_POSITION
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
      Config config = getConfiguration();
      Optional<ServerInfoWrapper> targetServer = getServer(config.TARGET_SERVER);
      if (targetServer.isEmpty())
        return;

      Map<QueueType, AtomicInteger> map = new HashMap<>();
      for (QueueType type : config.QUEUE_TYPES) {
        map.put(type, new AtomicInteger());
      }

      for (PlayerWrapper player : targetServer.get().getConnectedPlayers()) {
        QueueType playerType = config.getQueueType(player);

        AtomicInteger queueTypePlayers = map.get(playerType);
        if (queueTypePlayers != null) {
          queueTypePlayers.incrementAndGet();
        }
      }

      map.forEach((type, count) -> type.getPlayersWithTypeInTarget().set(count.get()));
    }, 0, 1, TimeUnit.SECONDS);
  }

  default void sendCustomData() {
    Config config = getConfiguration();
    List<PlayerWrapper> networkPlayers = getPlayers();

    if (networkPlayers == null || networkPlayers.isEmpty()) {
      return;
    }

    ByteArrayDataOutput outOnlineQueue = ByteStreams.newDataOutput();

    outOnlineQueue.writeUTF("onlineQueue");
    outOnlineQueue.writeInt(config.QUEUE_TYPES.length);
    for (QueueType queueType : config.QUEUE_TYPES) {
      outOnlineQueue.writeUTF(queueType.getName().toLowerCase(Locale.ROOT));
      outOnlineQueue.writeInt(queueType.getQueueMap().size());
    }

    ByteArrayDataOutput outOnlineTarget = ByteStreams.newDataOutput();

    outOnlineTarget.writeUTF("onlineTarget");
    outOnlineTarget.writeInt(config.QUEUE_TYPES.length);
    for (QueueType queueType : config.QUEUE_TYPES) {
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

      loadConfig(file);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  default void loadConfig(Path file) throws IOException {
    Config loaded = YamlConfigurations.update(
      file,
      Config.class,
      builder -> builder.setNameFormatter(NameFormatters.IDENTITY)
    );
    getConfiguration().copyFrom(loaded);
  }
}
