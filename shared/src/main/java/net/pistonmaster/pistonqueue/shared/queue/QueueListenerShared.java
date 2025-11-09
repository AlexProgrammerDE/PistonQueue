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

  protected void onPreLogin(PQPreLoginEvent event) {
    if (event.isCancelled())
      return;

    Config config = currentConfig();
    if (config.ENABLE_USERNAME_REGEX && !event.getUsername().matches(config.USERNAME_REGEX)) {
      event.setCancelled(config.USERNAME_REGEX_MESSAGE.replace("%regex%", config.USERNAME_REGEX));
    }
  }

  protected void onPostLogin(PlayerWrapper player) {
    Config config = currentConfig();
    if (StorageTool.isShadowBanned(player.getName()) && config.SHADOW_BAN_TYPE == BanType.KICK) {
      player.disconnect(config.SERVER_DOWN_KICK_MESSAGE);
    }
  }

  protected void onKick(PQKickedFromServerEvent event) {
    Config config = currentConfig();
    QueueGroup group = resolveGroupForTarget(event.getKickedFrom());
    boolean kickedFromProtectedTarget = group.getTargetServers().contains(event.getKickedFrom());
    if (config.IF_TARGET_DOWN_SEND_TO_QUEUE && kickedFromProtectedTarget) {
      String kickReason = event.getKickReason()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .orElse("unknown reason");

      config.DOWN_WORD_LIST.stream()
        .filter(word -> kickReason.contains(word.toLowerCase(Locale.ROOT)))
        .findFirst()
        .ifPresent(word -> {
          event.setCancelServer(group.getQueueServer());

          event.getPlayer().sendMessage(config.IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE);

          config.getQueueType(event.getPlayer())
            .getQueueMap()
            .put(event.getPlayer().getUniqueId(), new QueueType.QueuedPlayer(event.getKickedFrom(), QueueType.QueueReason.SERVER_DOWN));
        });
    }

    if (config.ENABLE_KICK_MESSAGE && event.willDisconnect()) {
      event.setKickMessage(config.KICK_MESSAGE);
    }
  }

  protected void onPreConnect(PQServerPreConnectEvent event) {
    PlayerWrapper player = event.getPlayer();
    Config config = currentConfig();
    QueueGroup targetGroup = event.getTarget()
      .flatMap(name -> config.findGroupByTarget(name))
      .orElse(defaultGroup());

    if (config.ENABLE_SOURCE_SERVER && !isSourceToTarget(event, targetGroup)) {
      return;
    }

    if (!config.ENABLE_SOURCE_SERVER && player.getCurrentServer().isPresent()) {
      return;
    }

    if (config.KICK_WHEN_DOWN) {
      for (String server : config.KICK_WHEN_DOWN_SERVERS) {
        if (!onlineServers.contains(server)) {
          player.disconnect(config.SERVER_DOWN_KICK_MESSAGE);
          return;
        }
      }
    }

    QueueType type = config.getQueueType(player);
    QueueGroup typeGroup = resolveGroupForType(type);

    boolean serverFull = false;
    if (config.ALWAYS_QUEUE || (serverFull = isServerFull(type))) {
      if (player.hasPermission(config.QUEUE_BYPASS_PERMISSION)) {
        event.setTarget(defaultTarget(typeGroup));
      } else {
        putQueue(player, typeGroup, type, event, serverFull, config);
      }
    }
  }

  private void putQueue(PlayerWrapper player, QueueGroup group, QueueType type, PQServerPreConnectEvent event, boolean serverFull, Config config) {
    player.sendPlayerList(type.getHeader(), type.getFooter());

    if (serverFull && !type.getQueueMap().containsKey(player.getUniqueId())) {
      player.sendMessage(config.SERVER_IS_FULL_MESSAGE);
    }

    Optional<String> originalTarget = event.getTarget();

    event.setTarget(group.getQueueServer());

    Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();

    String queueTarget;
    // Store the data concerning the player's original destination
    if (config.FORCE_TARGET_SERVER || originalTarget.isEmpty()) {
      queueTarget = defaultTarget(group);
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

  private boolean isSourceToTarget(PQServerPreConnectEvent event, QueueGroup group) {
    Optional<String> previousServer = event.getPlayer().getCurrentServer();
    return previousServer.isPresent()
      && group.getSourceServers().contains(previousServer.get())
      && event.getTarget().isPresent()
      && group.getTargetServers().contains(event.getTarget().get());
  }

  public void moveQueue() {
    Config config = currentConfig();
    for (QueueGroup group : config.getQueueGroups()) {
      cleanQueueForGroup(group);
    }

    if (config.RECOVERY) {
      plugin.getPlayers().forEach(this::doRecovery);
    }

    for (QueueGroup group : config.getQueueGroups()) {
      if (config.PAUSE_QUEUE_IF_TARGET_DOWN && !isGroupTargetOnline(group)) {
        continue;
      }
      for (QueueType type : group.getQueueTypes()) {
        connectPlayer(group, type);
      }
    }
  }

  private void cleanQueueForGroup(QueueGroup group) {
    for (QueueType type : group.getQueueTypes()) {
      Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();
      List<UUID> queueSnapshot;
      synchronized (queueMap) {
        queueSnapshot = new ArrayList<>(queueMap.keySet());
      }

      if (queueSnapshot.isEmpty()) {
        continue;
      }

      List<UUID> staleEntries = new ArrayList<>();
      for (UUID uuid : queueSnapshot) {
        Optional<PlayerWrapper> player = plugin.getPlayer(uuid);
        Optional<String> optionalTarget = player.flatMap(PlayerWrapper::getCurrentServer);
        if (optionalTarget.isEmpty() || !optionalTarget.get().equals(group.getQueueServer())) {
          staleEntries.add(uuid);
        }
      }

      if (!staleEntries.isEmpty()) {
        synchronized (queueMap) {
          staleEntries.forEach(queueMap::remove);
        }
      }
    }
  }

  private void doRecovery(PlayerWrapper player) {
    Config config = currentConfig();
    QueueType type = config.getQueueType(player);
    QueueGroup group = resolveGroupForType(type);

    Optional<String> currentServer = player.getCurrentServer();
    if (!type.getQueueMap().containsKey(player.getUniqueId())
      && currentServer.isPresent()
      && currentServer.get().equals(group.getQueueServer())) {
      type.getQueueMap().putIfAbsent(player.getUniqueId(), new QueueType.QueuedPlayer(defaultTarget(group), QueueType.QueueReason.RECOVERY));

      player.sendMessage(config.RECOVERY_MESSAGE);
    }
  }

  private void connectPlayer(QueueGroup group, QueueType type) {
    Config config = currentConfig();
    int freeSlots = getFreeSlots(type);

    if (freeSlots <= 0) {
      return;
    }

    if (freeSlots > config.MAX_PLAYERS_PER_MOVE) {
      freeSlots = config.MAX_PLAYERS_PER_MOVE;
    }

    int movesLeft = freeSlots;

    while (movesLeft > 0) {
      Map.Entry<UUID, QueueType.QueuedPlayer> entry = pollNextQueueEntry(type);
      if (entry == null) {
        break;
      }

      Optional<PlayerWrapper> optional = plugin.getPlayer(entry.getKey());
      if (optional.isEmpty()) {
        continue;
      }
      PlayerWrapper player = optional.get();

      player.sendMessage(config.JOINING_TARGET_SERVER);
      player.resetPlayerList();

      if (StorageTool.isShadowBanned(player.getName())
        && (config.SHADOW_BAN_TYPE == BanType.LOOP
        || (config.SHADOW_BAN_TYPE == BanType.PERCENT && ThreadLocalRandom.current().nextInt(100) >= config.PERCENT))) {
        player.sendMessage(config.SHADOW_BAN_MESSAGE);

        type.getQueueMap().put(entry.getKey(), entry.getValue());

        continue;
      }

      indexPositionTime(type);

      Map<Integer, Instant> cache = type.getPositionCache().get(entry.getKey());
      if (cache != null) {
        cache.forEach((position, instant) ->
          type.getDurationFromPosition().put(position, Duration.between(instant, Instant.now())));
      }

      player.connect(entry.getValue().targetServer());

      movesLeft--;
    }

    if (config.SEND_XP_SOUND) {
      sendXPSoundToQueueType(group, type);
    }
  }

  private void sendXPSoundToQueueType(QueueGroup group, QueueType type) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("xpV2");

    List<UUID> uuids = new ArrayList<>(5);
    Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();
    synchronized (queueMap) {
      for (UUID uuid : queueMap.keySet()) {
        uuids.add(uuid);
        if (uuids.size() == 5) {
          break;
        }
      }
    }

    out.writeInt(uuids.size());
    uuids.forEach(id -> out.writeUTF(id.toString()));

    plugin.getServer(group.getQueueServer()).ifPresent(server ->
      server.sendPluginMessage("piston:queue", out.toByteArray()));
  }

  private void indexPositionTime(QueueType type) {
    int position = 0;

    Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();
    synchronized (queueMap) {
      for (UUID uuid : queueMap.keySet()) {
        position++;
        Map<Integer, Instant> list = type.getPositionCache().get(uuid);
        if (list == null) {
          type.getPositionCache().put(uuid, new HashMap<>(Collections.singletonMap(position, Instant.now())));
        } else if (!list.containsKey(position)) {
          list.put(position, Instant.now());
        }
      }
    }
  }

  private Config currentConfig() {
    return plugin.getConfiguration();
  }

  private QueueGroup defaultGroup() {
    QueueGroup group = currentConfig().getDefaultGroup();
    if (group != null) {
      return group;
    }
    QueueType[] queueTypes = currentConfig().getAllQueueTypes().toArray(new QueueType[0]);
    return new QueueGroup(
      "default",
      currentConfig().QUEUE_SERVER,
      List.of(currentConfig().TARGET_SERVER),
      currentConfig().ENABLE_SOURCE_SERVER ? List.of(currentConfig().SOURCE_SERVER) : List.of(),
      queueTypes
    );
  }

  private QueueGroup resolveGroupForTarget(String server) {
    if (server == null) {
      return defaultGroup();
    }
    return currentConfig().findGroupByTarget(server).orElse(defaultGroup());
  }

  private QueueGroup resolveGroupForType(QueueType type) {
    QueueGroup group = currentConfig().getGroupFor(type);
    return group != null ? group : defaultGroup();
  }

  private String defaultTarget(QueueGroup group) {
    if (group.getTargetServers().isEmpty()) {
      return currentConfig().TARGET_SERVER;
    }
    return group.getTargetServers().get(0);
  }

  private boolean isGroupTargetOnline(QueueGroup group) {
    return group.getTargetServers().stream().anyMatch(onlineServers::contains);
  }

  private Map.Entry<UUID, QueueType.QueuedPlayer> pollNextQueueEntry(QueueType type) {
    Map<UUID, QueueType.QueuedPlayer> queueMap = type.getQueueMap();
    synchronized (queueMap) {
      Iterator<Map.Entry<UUID, QueueType.QueuedPlayer>> iterator = queueMap.entrySet().iterator();
      if (!iterator.hasNext()) {
        return null;
      }

      Map.Entry<UUID, QueueType.QueuedPlayer> entry = iterator.next();
      iterator.remove();
      return new AbstractMap.SimpleEntry<>(entry);
    }
  }
}
