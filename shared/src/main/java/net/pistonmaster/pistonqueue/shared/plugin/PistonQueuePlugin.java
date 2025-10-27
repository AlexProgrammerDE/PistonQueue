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
import net.pistonmaster.pistonqueue.shared.loadbalance.EndpointConfig;
import net.pistonmaster.pistonqueue.shared.loadbalance.EndpointMode;
import net.pistonmaster.pistonqueue.shared.loadbalance.EndpointSelector;
import net.pistonmaster.pistonqueue.shared.loadbalance.LobbyGroupConfig;
import net.pistonmaster.pistonqueue.shared.loadbalance.SelectionOptions;
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

  /**
   * Connect player using either traditional proxy routing or the configured lobby group with transfer support.
   * Returns true if a connection/transfer was initiated, false if not possible (caller may initiate recovery).
   */
  default boolean connectPlayerToTarget(PlayerWrapper player, String defaultTargetServer) {
    info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    info("🔌 connectPlayerToTarget() CALLED");
    info("Player: " + player.getName());
    info("Default target: " + defaultTargetServer);
    info("USE_TARGET_LOBBY_GROUP: " + Config.USE_TARGET_LOBBY_GROUP);
    info("TARGET_LOBBY_GROUP: " + Config.TARGET_LOBBY_GROUP);
    info("LOBBY_GROUPS loaded: " + (Config.LOBBY_GROUPS != null ? Config.LOBBY_GROUPS.size() + " groups" : "null"));
    info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    
    if (Config.USE_TARGET_LOBBY_GROUP && Config.TARGET_LOBBY_GROUP != null && Config.LOBBY_GROUPS != null) {
      info("Player " + player.getName() + " using lobby group mode. Group: " + Config.TARGET_LOBBY_GROUP);
      LobbyGroupConfig group = Config.LOBBY_GROUPS.get(Config.TARGET_LOBBY_GROUP);
      if (group != null) {
        info("Selecting endpoint from group '" + Config.TARGET_LOBBY_GROUP + "' with " + group.getEndpoints().size() + " endpoints");
        Optional<EndpointConfig> pick = EndpointSelector.select(this, group);
        if (pick.isPresent()) {
          EndpointConfig ep = pick.get();
          info("Selected endpoint: name=" + ep.getName() + ", mode=" + ep.getMode() + ", priority=" + ep.getPriority() + ", weight=" + ep.getWeight());
          
          EndpointMode mode = ep.getMode();
          // Transport override handling
          switch (group.getSelection().getTransportOverride()) {
            case FORCE_VELOCITY -> {
              info("Transport override FORCE_VELOCITY applied");
              mode = EndpointMode.VELOCITY;
            }
            case FORCE_TRANSFER -> {
              info("Transport override FORCE_TRANSFER applied");
              mode = EndpointMode.TRANSFER;
            }
            default -> {}
          }

          if (mode == EndpointMode.VELOCITY) {
            if (ep.getVelocityServer() == null || ep.getVelocityServer().isBlank()) {
              warning("Selected VELOCITY endpoint '" + ep.getName() + "' has no server name; falling back to default target: " + defaultTargetServer);
              player.connect(defaultTargetServer);
              return true;
            }
            info("Connecting player " + player.getName() + " to VELOCITY server: " + ep.getVelocityServer());
            player.connect(ep.getVelocityServer());
            return true;
          } else {
            // TRANSFER path
            if (ep.getHost() == null || ep.getPort() <= 0) {
              warning("Selected TRANSFER endpoint '" + ep.getName() + "' has invalid host/port (host=" + ep.getHost() + ", port=" + ep.getPort() + "); falling back to default target: " + defaultTargetServer);
              player.connect(defaultTargetServer);
              return true;
            }
            int minProto = Math.max(0, Config.TRANSFER_MIN_PROTOCOL);
            Optional<Integer> clientProto = player.getProtocolVersion();
            info("Attempting TRANSFER for player " + player.getName() + " to " + ep.getHost() + ":" + ep.getPort() + " (client protocol: " + clientProto.orElse(-1) + ", min required: " + minProto + ")");
            
            if (minProto > 0 && clientProto.isPresent() && clientProto.get() < minProto) {
              warning("Player " + player.getName() + " protocol=" + clientProto.get() + " below min transfer protocol=" + minProto + ". Using proxy connect fallback to: " + defaultTargetServer);
              player.connect(defaultTargetServer);
              return true;
            }
            boolean ok = player.transfer(ep.getHost(), ep.getPort());
            if (ok) {
              info("Transfer successful for player " + player.getName() + " to " + ep.getHost() + ":" + ep.getPort());
              EndpointSelector.noteTransfer(ep, group.getSelection().getCacheTtlMs());
              return true;
            } else {
              warning("Transfer not supported or failed for player " + player.getName() + " to " + ep.getHost() + ":" + ep.getPort() + ". Fallback policy: " + group.getSelection().getFallbackOnTransferFail());
              if (group.getSelection().getFallbackOnTransferFail() == net.pistonmaster.pistonqueue.shared.loadbalance.TransferFallback.PROXY_CONNECT) {
                info("Using proxy connect fallback to: " + defaultTargetServer);
                player.connect(defaultTargetServer);
                return true;
              } else {
                warning("Transfer ABORT - player " + player.getName() + " will remain in queue for retry");
                return false; // ABORT, let caller requeue or handle
              }
            }
          }
        } else {
          warning("No online endpoints available in lobby group '" + Config.TARGET_LOBBY_GROUP + "'. Checked " + (group.getEndpoints() != null ? group.getEndpoints().size() : 0) + " endpoints. Using default target server: " + defaultTargetServer);
        }
      } else {
        warning("Lobby group '" + Config.TARGET_LOBBY_GROUP + "' not found in LOBBY_GROUPS configuration. Using default target server: " + defaultTargetServer);
      }
    }
    // Default legacy behavior
    info("Using legacy connection mode for player " + player.getName() + " to server: " + defaultTargetServer);
    player.connect(defaultTargetServer);
    return true;
  }

  default void scheduleTasks(QueueListenerShared queueListener) {
    // Sends the position message and updates tab on an interval in chat
    schedule(() -> {
      // Skip TARGET_SERVER check if using lobby groups
      boolean targetServerOnline = Config.USE_TARGET_LOBBY_GROUP || queueListener.getOnlineServers().contains(Config.TARGET_SERVER);
      
      if (targetServerOnline) {
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
      
      // If using lobby groups, don't check TARGET_SERVER
      if (Config.USE_TARGET_LOBBY_GROUP) {
        servers.remove(Config.TARGET_SERVER);
      }
      
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
      for (QueueType type : Config.QUEUE_TYPES) {
        map.put(type, new AtomicInteger());
      }

      for (PlayerWrapper player : targetServer.get().getConnectedPlayers()) {
        QueueType playerType = QueueType.getQueueType(player);

        AtomicInteger queueTypePlayers = map.get(playerType);
        if (queueTypePlayers != null) {
          queueTypePlayers.incrementAndGet();
        }
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
        } else if (fieldName.equals("LOBBY_GROUPS")) {
          // Parse nested lobby groups
          Map<Object, ? extends ConfigurationNode> groups = config.node("LOBBY_GROUPS").childrenMap();
          if (groups == null || groups.isEmpty()) {
            it.set(Config.class, null);
          } else {
            Map<String, LobbyGroupConfig> map = new HashMap<>();
            for (Map.Entry<Object, ? extends ConfigurationNode> e : groups.entrySet()) {
              String name = String.valueOf(e.getKey());
              ConfigurationNode g = e.getValue();
              LobbyGroupConfig lg = new LobbyGroupConfig();
              lg.setName(name);
              // selection
              SelectionOptions sel = new SelectionOptions();
              ConfigurationNode selNode = g.node("selection");
              String tie = selNode.node("tieBreaker").getString();
              if (tie != null) {
                try {
                  sel.setTieBreaker(net.pistonmaster.pistonqueue.shared.loadbalance.TieBreaker.valueOf(tie.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                  sel.setTieBreaker(net.pistonmaster.pistonqueue.shared.loadbalance.TieBreaker.LEAST_PLAYERS);
                }
              }
              String pcs = selNode.node("playerCountSource").getString();
              if (pcs != null) {
                try {
                  sel.setPlayerCountSource(net.pistonmaster.pistonqueue.shared.loadbalance.PlayerCountSource.valueOf(pcs.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                  sel.setPlayerCountSource(net.pistonmaster.pistonqueue.shared.loadbalance.PlayerCountSource.AUTO);
                }
              }
              String tovr = selNode.node("transportOverride").getString();
              if (tovr != null) {
                try {
                  sel.setTransportOverride(net.pistonmaster.pistonqueue.shared.loadbalance.TransportOverride.valueOf(tovr.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                  sel.setTransportOverride(net.pistonmaster.pistonqueue.shared.loadbalance.TransportOverride.PER_ENDPOINT);
                }
              }
              String fb = selNode.node("fallbackOnTransferFail").getString();
              if (fb != null) {
                try {
                  sel.setFallbackOnTransferFail(net.pistonmaster.pistonqueue.shared.loadbalance.TransferFallback.valueOf(fb.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                  sel.setFallbackOnTransferFail(net.pistonmaster.pistonqueue.shared.loadbalance.TransferFallback.PROXY_CONNECT);
                }
              }
              sel.setPingTimeoutMs(selNode.node("pingTimeoutMs").getInt(750));
              sel.setCacheTtlMs(selNode.node("cacheTtlMs").getInt(2000));
              lg.setSelection(sel);
              // endpoints
              List<EndpointConfig> eps = new ArrayList<>();
              ConfigurationNode endpointsNode = g.node("endpoints");
              if (!endpointsNode.isList()) {
                warning("LOBBY_GROUPS." + name + ".endpoints is not a list; skipping group.");
                continue;
              }
              for (ConfigurationNode epNode : endpointsNode.childrenList()) {
                EndpointConfig ep = new EndpointConfig();
                ep.setName(epNode.node("name").getString("endpoint"));
                String mode = epNode.node("mode").getString();
                if (mode != null) {
                  try {
                    ep.setMode(EndpointMode.valueOf(mode.toUpperCase(Locale.ROOT)));
                  } catch (IllegalArgumentException ex) {
                    ep.setMode(EndpointMode.VELOCITY);
                  }
                }
                ep.setPriority(epNode.node("priority").getInt(1));
                ep.setWeight(epNode.node("weight").getInt(1));
                ep.setVelocityServer(epNode.node("velocityServer").getString());
                ep.setHost(epNode.node("host").getString());
                ep.setPort(epNode.node("port").getInt(0));
                eps.add(ep);
              }
              lg.setEndpoints(eps);
              map.put(name, lg);
            }
            it.set(Config.class, map);
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
    // Only add TARGET_SERVER to health check list if lobby groups are disabled
    if (!Config.USE_TARGET_LOBBY_GROUP && !Config.KICK_WHEN_DOWN_SERVERS.contains(Config.TARGET_SERVER)) {
      Config.KICK_WHEN_DOWN_SERVERS.add(Config.TARGET_SERVER);
    }
  }
}
