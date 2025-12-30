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
import net.pistonmaster.pistonqueue.shared.config.ConfigMigrator;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
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
import java.util.concurrent.locks.Lock;
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
    QueueGroup resolvedDefaultGroup = config.getDefaultGroup();
    if (resolvedDefaultGroup == null) {
      List<QueueType> queueTypes = config.getAllQueueTypes();
      resolvedDefaultGroup = new QueueGroup(
        "default",
        Collections.singletonList(config.queueServer()),
        Collections.singletonList(config.targetServer()),
        config.enableSourceServer() ? Collections.singletonList(config.sourceServer()) : Collections.emptyList(),
        queueTypes
      );
    }
    final QueueGroup defaultGroup = resolvedDefaultGroup;
    // Sends the position message and updates tab on an interval in chat
    schedule(() -> {
      boolean targetsOnline = defaultGroup.targetServers().stream().anyMatch(queueListener.getServerStatusManager().getOnlineServers()::contains);
      if (targetsOnline) {
        for (QueueType type : config.getAllQueueTypes()) {
          if (config.positionMessageChat()) {
            sendMessage(type, MessageType.CHAT);
          }
          if (config.positionMessageHotBar()) {
            sendMessage(type, MessageType.ACTION_BAR);
          }
        }
      } else if (config.pauseQueueIfTargetDown()) {
        for (QueueType type : config.getAllQueueTypes()) {
          for (UUID uuid : snapshotQueueOrder(type)) {
            getPlayer(uuid).ifPresent(value -> value.sendMessage(config.pauseQueueIfTargetDownMessage()));
          }
        }
      }
    }, config.positionMessageDelay(), config.positionMessageDelay(), TimeUnit.MILLISECONDS);

    // Updates the tab
    schedule(() -> {
      if (!config.positionPlayerList()) {
        return;
      }

      for (QueueType type : config.getAllQueueTypes()) {
        updateTab(type);
      }
    }, config.queueMoveDelay(), config.queueMoveDelay(), TimeUnit.MILLISECONDS);

    // Send plugin message
    schedule(this::sendCustomData, config.queueMoveDelay(), config.queueMoveDelay(), TimeUnit.MILLISECONDS);

    // Moves the queue when someone logs off the target server on an interval set in the config.yml
    schedule(queueListener::moveQueue, config.queueMoveDelay(), config.queueMoveDelay(), TimeUnit.MILLISECONDS);

    // Checks the status of all the servers
    schedule(() -> {
      List<String> servers = new ArrayList<>(config.kickWhenDownServers());
      CountDownLatch latch = new CountDownLatch(servers.size());
      for (String server : servers) {
        CompletableFuture.runAsync(() -> {
          try {
            Optional<ServerInfoWrapper> serverInfoWrapper = getServer(server);

            if (serverInfoWrapper.isPresent()) {
              if (serverInfoWrapper.get().isOnline()) {
                queueListener.getServerStatusManager().online(server);
              } else {
                warning("Server %s is down!!!".formatted(server));
                queueListener.getServerStatusManager().offline(server);
              }
            } else {
              warning("Server \"%s\" not set up!!! Check out: https://github.com/AlexProgrammerDE/PistonQueue/wiki/FAQ#server-not-set-up-error".formatted(server));
            }
          } finally {
            latch.countDown();
          }
        }).exceptionally(ex -> {
          error("Failed to check status of server " + server + ": " + ex.getMessage());
          return null;
        });
      }
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        error("Server status check interrupted: " + e.getMessage());
      }
    }, 500, config.serverOnlineCheckDelay(), TimeUnit.MILLISECONDS);
  }

  default void sendMessage(QueueType queue, MessageType type) {
    Config config = getConfiguration();
    List<UUID> queueOrder = snapshotQueueOrder(queue);

    int totalQueued = queueOrder.size();
    int position = 0;
    for (UUID uuid : queueOrder) {
      Optional<PlayerWrapper> player = getPlayer(uuid);
      if (player.isEmpty()) {
        continue;
      }

      String chatMessage = config.queuePosition()
        .replace("%position%", String.valueOf(++position))
        .replace("%total%", String.valueOf(totalQueued));

      player.get().sendMessage(type, chatMessage);
    }
  }

  default void updateTab(QueueType queue) {
    List<UUID> queueOrder = snapshotQueueOrder(queue);

    int position = 0;
    for (UUID uuid : queueOrder) {
      Optional<PlayerWrapper> optionalPlayer = getPlayer(uuid);
      if (optionalPlayer.isEmpty()) {
        continue;
      }
      PlayerWrapper player = optionalPlayer.get();
      int incrementedPosition = ++position;

      player.sendPlayerList(
        queue.getHeader().stream().map(str -> replacePosition(str, incrementedPosition, queue)).collect(Collectors.toList()),
        queue.getFooter().stream().map(str -> replacePosition(str, incrementedPosition, queue)).collect(Collectors.toList()));
    }
  }

  default String replacePosition(String text, int position, QueueType type) {
    Duration durationForExactPosition = null;
    Integer biggestPosition = null;
    Duration biggestDuration = null;

    Lock readLock = type.getDurationLock().readLock();
    readLock.lock();
    try {
      durationForExactPosition = type.getDurationFromPosition().get(position);
      if (durationForExactPosition == null) {
        for (Map.Entry<Integer, Duration> entry : type.getDurationFromPosition().entrySet()) {
          if (biggestPosition == null || entry.getKey() > biggestPosition) {
            biggestPosition = entry.getKey();
            biggestDuration = entry.getValue();
          }
        }
      }
    } finally {
      readLock.unlock();
    }

    if (durationForExactPosition != null) {
      return SharedChatUtils.formatDuration(text, durationForExactPosition, position);
    }

    Duration predictedDuration = biggestDuration == null
      ? Duration.of(position, ChronoUnit.MINUTES)
      : biggestDuration.plus(position - biggestPosition, ChronoUnit.MINUTES);

    return SharedChatUtils.formatDuration(text, predictedDuration, position);
  }

  default void initializeReservationSlots() {
    schedule(() -> {
      Config config = getConfiguration();
      for (QueueGroup group : config.getQueueGroups()) {
        Map<QueueType, AtomicInteger> map = new HashMap<>();
        for (QueueType type : group.queueTypes()) {
          map.put(type, new AtomicInteger());
        }

        for (String targetServerName : group.targetServers()) {
          getServer(targetServerName).ifPresent(targetServer -> {
            for (PlayerWrapper player : targetServer.getConnectedPlayers()) {
              QueueType playerType = config.getQueueType(player);

              AtomicInteger queueTypePlayers = map.get(playerType);
              if (queueTypePlayers != null) {
                queueTypePlayers.incrementAndGet();
              }
            }
          });
        }

        map.forEach((type, count) -> type.getPlayersWithTypeInTarget().set(count.get()));
      }
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
    outOnlineQueue.writeInt(config.getAllQueueTypes().size());
    for (QueueType queueType : config.getAllQueueTypes()) {
      outOnlineQueue.writeUTF(queueType.getName().toLowerCase(Locale.ROOT));
      outOnlineQueue.writeInt(getQueueSize(queueType));
    }

    ByteArrayDataOutput outOnlineTarget = ByteStreams.newDataOutput();

    outOnlineTarget.writeUTF("onlineTarget");
    outOnlineTarget.writeInt(config.getAllQueueTypes().size());
    for (QueueType queueType : config.getAllQueueTypes()) {
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
      error("Failed to process config: " + e.getMessage());
    }
  }

  default void loadConfig(Path file) throws IOException {
    ConfigMigrator.migrate(file);
    Config loaded = YamlConfigurations.update(
      file,
      Config.class,
      builder -> builder.setNameFormatter(NameFormatters.IDENTITY)
    );
    getConfiguration().copyFrom(loaded);
  }

  private static List<UUID> snapshotQueueOrder(QueueType queue) {
    Lock readLock = queue.getQueueLock().readLock();
    readLock.lock();
    try {
      return new ArrayList<>(queue.getQueueMap().keySet());
    } finally {
      readLock.unlock();
    }
  }

  private static int getQueueSize(QueueType queue) {
    Lock readLock = queue.getQueueLock().readLock();
    readLock.lock();
    try {
      return queue.getQueueMap().size();
    } finally {
      readLock.unlock();
    }
  }
}
