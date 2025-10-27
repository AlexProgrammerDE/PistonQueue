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
package net.pistonmaster.pistonqueue.shared.queue;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;
import net.pistonmaster.pistonqueue.shared.events.PQPreLoginEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public abstract class QueueListenerShared {
  private final PistonQueuePlugin plugin;
  @Getter
  private final Set<String> onlineServers = Collections.synchronizedSet(new HashSet<>());
  // Track recent transfers to prevent immediate recovery after transfer disconnect
  private final Map<UUID, Instant> recentTransfers = Collections.synchronizedMap(new HashMap<>());
  private static final Duration TRANSFER_COOLDOWN = Duration.ofSeconds(10);
  
  // Track queue join time for minimum queue time enforcement
  private final Map<UUID, Instant> queueJoinTimes = Collections.synchronizedMap(new HashMap<>());

  /**
   * Mark a player as recently transferred to prevent recovery from adding them back to queue
   */
  public void markRecentTransfer(UUID playerId) {
    recentTransfers.put(playerId, Instant.now());
    plugin.info("Marked player " + playerId + " as recently transferred (10s cooldown)");
  }

  protected void onPreLogin(PQPreLoginEvent event) {
    if (event.isCancelled())
      return;

    if (Config.ENABLE_USERNAME_REGEX && !event.getUsername().matches(Config.USERNAME_REGEX)) {
      event.setCancelled(Config.USERNAME_REGEX_MESSAGE.replace("%regex%", Config.USERNAME_REGEX));
    }
  }

  protected void onPostLogin(PlayerWrapper player) {
    if (StorageTool.isShadowBanned(player.getName()) && Config.SHADOW_BAN_TYPE == BanType.KICK) {
      player.disconnect(Config.SERVER_DOWN_KICK_MESSAGE);
    }
  }

  protected void onKick(PQKickedFromServerEvent event) {
    if (Config.IF_TARGET_DOWN_SEND_TO_QUEUE && event.getKickedFrom().equals(Config.TARGET_SERVER)) {
      String kickReason = event.getKickReason()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .orElse("unknown reason");

      Config.DOWN_WORD_LIST.stream()
        .filter(word -> kickReason.contains(word.toLowerCase(Locale.ROOT)))
        .findFirst()
        .ifPresent(word -> {
          event.setCancelServer(Config.QUEUE_SERVER);

          event.getPlayer().sendMessage(Config.IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE);

          QueueType.getQueueType(event.getPlayer())
            .getQueueMap()
            .put(event.getPlayer().getUniqueId(), new QueueType.QueuedPlayer(event.getKickedFrom(), QueueType.QueueReason.SERVER_DOWN));
        });
    }

    if (Config.ENABLE_KICK_MESSAGE && event.willDisconnect()) {
      event.setKickMessage(Config.KICK_MESSAGE);
    }
  }

  protected void onPreConnect(PQServerPreConnectEvent event) {
    PlayerWrapper player = event.getPlayer();

    if (Config.ENABLE_SOURCE_SERVER && !isSourceToTarget(event)) {
      return;
    }

    if (!Config.ENABLE_SOURCE_SERVER && player.getCurrentServer().isPresent()) {
      return;
    }

    if (Config.KICK_WHEN_DOWN) {
      for (String server : Config.KICK_WHEN_DOWN_SERVERS) {
        if (!onlineServers.contains(server)) {
          player.disconnect(Config.SERVER_DOWN_KICK_MESSAGE);
          return;
        }
      }
    }

    QueueType type = QueueType.getQueueType(player);

    boolean serverFull = false;
    if (Config.ALWAYS_QUEUE || (serverFull = isServerFull(type))) {
      if (player.hasPermission(Config.QUEUE_BYPASS_PERMISSION)) {
        event.setTarget(Config.TARGET_SERVER);
      } else {
        putQueue(player, type, event, serverFull);
      }
    }
  }

  private void putQueue(PlayerWrapper player, QueueType type, PQServerPreConnectEvent event, boolean serverFull) {
    player.sendPlayerList(type.getHeader(), type.getFooter());

    if (serverFull && !type.getQueueMap().containsKey(player.getUniqueId())) {
      player.sendMessage(Config.SERVER_IS_FULL_MESSAGE);
    }

    // Redirect the player to the queue.
    Optional<String> originalTarget = event.getTarget();

    event.setTarget(Config.QUEUE_SERVER);

    Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();

    String queueTarget;
    // Store the data concerning the player's original destination
    if (Config.FORCE_TARGET_SERVER || originalTarget.isEmpty()) {
      queueTarget = Config.TARGET_SERVER;
    } else {
      queueTarget = originalTarget.get();
    }

    queueMap.putIfAbsent(player.getUniqueId(), new QueueType.QueuedPlayer(queueTarget, QueueType.QueueReason.SERVER_FULL));
  }

  private boolean isServerFull(QueueType type) {
    return isTargetFull(type) || isAnyoneQueuedOfType(type);
  }

  private boolean isTargetFull(QueueType type) {
    return getFreeSlots(type) <= 0;
  }

  private int getFreeSlots(QueueType type) {
    return type.getReservedSlots() - type.getPlayersWithTypeInTarget().get();
  }

  private boolean isAnyoneQueuedOfType(QueueType type) {
    return !type.getQueueMap().isEmpty();
  }

  private boolean isSourceToTarget(PQServerPreConnectEvent event) {
    Optional<String> previousServer = event.getPlayer().getCurrentServer();
    return previousServer.isPresent() && previousServer.get().equals(Config.SOURCE_SERVER)
      && event.getTarget().isPresent() && event.getTarget().get().equals(Config.TARGET_SERVER);
  }

  public void moveQueue() {
    // Calculate total queued players
    int totalQueued = Arrays.stream(Config.QUEUE_TYPES)
      .mapToInt(type -> type.getQueueMap().size())
      .sum();
    
    // Only show detailed logs if there are queued players
    if (totalQueued > 0) {
      plugin.info("════════════════════════════════════════════════════════════");
      plugin.info("🔄 moveQueue() - " + totalQueued + " player(s) in queue");
      plugin.info("════════════════════════════════════════════════════════════");
    }
    
    for (QueueType type : Config.QUEUE_TYPES) {
      if (totalQueued > 0 && type.getQueueMap().size() > 0) {
        plugin.info("Queue type: " + type.getName() + " has " + type.getQueueMap().size() + " player(s)");
      }
      for (Map.Entry<UUID, QueueType.QueuedPlayer> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
        Optional<PlayerWrapper> player = plugin.getPlayer(entry.getKey());

        Optional<String> optionalTarget = player.flatMap(PlayerWrapper::getCurrentServer);
        if (optionalTarget.isEmpty() || !optionalTarget.get().equals(Config.QUEUE_SERVER)) {
          plugin.info("Removing player " + entry.getKey() + " from queue (not on queue server)");
          type.getQueueMap().remove(entry.getKey());
          queueJoinTimes.remove(entry.getKey()); // Cleanup join time tracking
        }
      }
    }

    if (Config.RECOVERY) {
      plugin.getPlayers().forEach(this::doRecovery);
    }

    // Only log config status if there are queued players
    if (totalQueued > 0) {
      plugin.info("PAUSE_QUEUE_IF_TARGET_DOWN: " + Config.PAUSE_QUEUE_IF_TARGET_DOWN);
      plugin.info("USE_TARGET_LOBBY_GROUP: " + Config.USE_TARGET_LOBBY_GROUP);
      plugin.info("TARGET_SERVER: " + Config.TARGET_SERVER);
      plugin.info("TARGET_SERVER online: " + onlineServers.contains(Config.TARGET_SERVER));
      plugin.info("Online servers: " + onlineServers);
    }
    
    if (Config.PAUSE_QUEUE_IF_TARGET_DOWN && !Config.USE_TARGET_LOBBY_GROUP && !onlineServers.contains(Config.TARGET_SERVER)) {
      if (totalQueued > 0) {
        plugin.warning("⚠️ Queue PAUSED: TARGET_SERVER '" + Config.TARGET_SERVER + "' is offline and PAUSE_QUEUE_IF_TARGET_DOWN is enabled");
        plugin.warning("To use lobby groups, the TARGET_SERVER check is skipped when USE_TARGET_LOBBY_GROUP is true");
      }
      return;
    }

    if (totalQueued > 0) {
      plugin.info("Proceeding to connect players from queue...");
    }
    Arrays.stream(Config.QUEUE_TYPES).forEachOrdered(this::connectPlayer);
    if (totalQueued > 0) {
      plugin.info("moveQueue() completed");
    }
  }

  private void doRecovery(PlayerWrapper player) {
    QueueType type = QueueType.getQueueType(player);

    Optional<String> currentServer = player.getCurrentServer();
    if (!type.getQueueMap().containsKey(player.getUniqueId()) && currentServer.isPresent() && currentServer.get().equals(Config.QUEUE_SERVER)) {
      // Check if player was recently transferred (within cooldown period)
      Instant lastTransfer = recentTransfers.get(player.getUniqueId());
      if (lastTransfer != null) {
        Duration timeSinceTransfer = Duration.between(lastTransfer, Instant.now());
        if (timeSinceTransfer.compareTo(TRANSFER_COOLDOWN) < 0) {
          long secondsRemaining = TRANSFER_COOLDOWN.getSeconds() - timeSinceTransfer.getSeconds();
          plugin.info("Skipping RECOVERY for player " + player.getName() + " - was transferred " + timeSinceTransfer.getSeconds() + "s ago (cooldown: " + secondsRemaining + "s remaining)");
          return;
        } else {
          // Cooldown expired, remove from tracking
          recentTransfers.remove(player.getUniqueId());
        }
      }
      
      // Only send RECOVERY_MESSAGE if player was actually in queue before (genuine recovery case)
      // Don't send message on first join to queue server
      boolean wasInQueue = type.getPositionCache().containsKey(player.getUniqueId());
      
      type.getQueueMap().putIfAbsent(player.getUniqueId(), new QueueType.QueuedPlayer(Config.TARGET_SERVER, QueueType.QueueReason.RECOVERY));
      
      // Track queue join time for minimum queue time enforcement
      if (!wasInQueue && Config.MINIMUM_QUEUE_TIME_ENABLED) {
        queueJoinTimes.put(player.getUniqueId(), Instant.now());
      }

      if (wasInQueue) {
        // Genuine recovery: player was queued before but lost connection
        player.sendMessage(Config.RECOVERY_MESSAGE);
      }
    }
  }

  private void connectPlayer(QueueType type) {
    int freeSlots = getFreeSlots(type);

    // Only log if queue is not empty
    if (type.getQueueMap().size() > 0) {
      plugin.info("┌─────────────────────────────────────────────────────────┐");
      plugin.info("│ Processing queue type: " + type.getName());
      plugin.info("└─────────────────────────────────────────────────────────┘");
      plugin.info("Free slots: " + freeSlots + ", Queue size: " + type.getQueueMap().size());
    }

    if (freeSlots <= 0) {
      if (type.getQueueMap().size() > 0) {
        plugin.warning("⚠️ No free slots available for " + type.getName() + " - queue paused");
      }
      return;
    }

    if (freeSlots > Config.MAX_PLAYERS_PER_MOVE)
      freeSlots = Config.MAX_PLAYERS_PER_MOVE;

    if (type.getQueueMap().size() > 0) {
      plugin.info("Will attempt to move up to " + freeSlots + " player(s)");
    }

  boolean movedAny = false; // Track if we actually initiated any connection this tick

  for (Map.Entry<UUID, QueueType.QueuedPlayer> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
      Optional<PlayerWrapper> optional = plugin.getPlayer(entry.getKey());
      if (optional.isEmpty()) {
        plugin.info("Player " + entry.getKey() + " not found online, skipping");
        continue;
      }
      PlayerWrapper player = optional.get();
      
      // Check minimum queue time
      if (Config.MINIMUM_QUEUE_TIME_ENABLED && Config.MINIMUM_QUEUE_TIME_SECONDS > 0) {
        Instant joinTime = queueJoinTimes.get(player.getUniqueId());
        if (joinTime != null) {
          Duration timeInQueue = Duration.between(joinTime, Instant.now());
          long requiredSeconds = Config.MINIMUM_QUEUE_TIME_SECONDS;
          if (timeInQueue.getSeconds() < requiredSeconds) {
            long remaining = requiredSeconds - timeInQueue.getSeconds();
            plugin.info("Player " + player.getName() + " has only been in queue for " + timeInQueue.getSeconds() + "s (minimum: " + requiredSeconds + "s, remaining: " + remaining + "s)");
            continue; // Skip this player, try next
          }
          // Minimum time satisfied, remove from tracking
          queueJoinTimes.remove(player.getUniqueId());
        }
      }

      type.getQueueMap().remove(entry.getKey());

      player.sendMessage(Config.JOINING_TARGET_SERVER);
      player.resetPlayerList();

      if (StorageTool.isShadowBanned(player.getName())
        && (Config.SHADOW_BAN_TYPE == BanType.LOOP
        || (Config.SHADOW_BAN_TYPE == BanType.PERCENT && ThreadLocalRandom.current().nextInt(100) >= Config.PERCENT))) {
        player.sendMessage(Config.SHADOW_BAN_MESSAGE);

        type.getQueueMap().put(entry.getKey(), entry.getValue());

        continue;
      }

      indexPositionTime(type);

      Map<Integer, Instant> cache = type.getPositionCache().get(entry.getKey());
      if (cache != null) {
        cache.forEach((position, instant) ->
          type.getDurationFromPosition().put(position, Duration.between(instant, Instant.now())));
      }

      plugin.info("=== Starting connection attempt for player: " + player.getName() + " ===");
      plugin.info("Target server from queue entry: " + entry.getValue().targetServer());
      plugin.info("Queue reason: " + entry.getValue().queueReason());
      
      // Set ThreadLocal so connectPlayerToTarget can call back to mark transfer
      net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin.QUEUE_LISTENER.set(this);
      try {
        boolean started = plugin.connectPlayerToTarget(player, entry.getValue().targetServer());
      
        if (!started) {
          // Transfer aborted: put the player back into the queue (recovery-like behavior)
          plugin.warning("❌ connectPlayerToTarget returned FALSE for player " + player.getName() + ". Returning to queue.");
          type.getQueueMap().put(entry.getKey(), entry.getValue());
          continue;
        } else {
          plugin.info("✅ Connection initiated for player " + player.getName());
          movedAny = true;
        }
      } finally {
        net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin.QUEUE_LISTENER.remove();
      }

      if (--freeSlots <= 0) {
        break;
      }
    }

    if (Config.SEND_XP_SOUND && movedAny) {
      // Only play XP sound when the queue actually progressed this tick
      sendXPSoundToQueueType(type);
    }
  }

  private void sendXPSoundToQueueType(QueueType type) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("xpV2");

    List<UUID> uuids = type.getQueueMap().keySet()
      .stream()
      .limit(5)
      .toList();

    out.writeInt(uuids.size());
    uuids.forEach(id -> out.writeUTF(id.toString()));

    plugin.getServer(Config.QUEUE_SERVER).ifPresent(server ->
      server.sendPluginMessage("piston:queue", out.toByteArray()));
  }

  private void indexPositionTime(QueueType type) {
    int position = 0;

    for (UUID uuid : new LinkedHashMap<>(type.getQueueMap()).keySet()) {
      position++;
      Map<Integer, Instant> list = type.getPositionCache().get(uuid);
      if (list == null) {
        type.getPositionCache().put(uuid, new HashMap<>(Collections.singletonMap(position, Instant.now())));
      } else {
        if (!list.containsKey(position)) {
          list.put(position, Instant.now());
        }
      }
    }
  }
}
