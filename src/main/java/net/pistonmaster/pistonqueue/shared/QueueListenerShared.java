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
package net.pistonmaster.pistonqueue.shared;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;
import net.pistonmaster.pistonqueue.shared.events.PQPreLoginEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerConnectedEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.utils.BanType;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public abstract class QueueListenerShared {
    private final PistonQueuePlugin plugin;
    @Setter
    @Getter
    protected boolean mainOnline = false;
    @Setter
    protected boolean queueOnline = false;
    @Setter
    protected boolean authOnline = false;
    @Setter
    protected Instant onlineSince = null;

    protected void onPreLogin(PQPreLoginEvent event) {
        if (event.isCancelled())
            return;

        if (Config.ENABLE_USERNAME_REGEX && !event.getUsername().matches(Config.USERNAME_REGEX)) {
            event.setCancelled(Config.USERNAME_REGEX_MESSAGE.replace("%regex%", Config.USERNAME_REGEX));
        }
    }

    protected void onPostLogin(PlayerWrapper player) {
        if (StorageTool.isShadowBanned(player.getUniqueId()) && Config.SHADOW_BAN_TYPE == BanType.KICK) {
            player.disconnect(Config.SERVER_DOWN_KICK_MESSAGE);
        }
    }

    protected void onKick(PQKickedFromServerEvent event) {
        if (Config.IF_MAIN_DOWN_SEND_TO_QUEUE && event.getKickedFrom().equals(Config.MAIN_SERVER)) {
            Optional<String> optionalKickReason = event.getKickReason();

            if (optionalKickReason.isPresent()) {
                for (String str : Config.DOWN_WORD_LIST) {
                    if (!optionalKickReason.get().toLowerCase().contains(str))
                        continue;

                    event.setCancelServer(Config.QUEUE_SERVER);

                    event.getPlayer().sendMessage(Config.IF_MAIN_DOWN_SEND_TO_QUEUE_MESSAGE);

                    QueueType.getQueueType(event.getPlayer()::hasPermission).getQueueMap().put(event.getPlayer().getUniqueId(), event.getKickedFrom());
                    break;
                }
            }
        }

        if (Config.ENABLE_KICK_MESSAGE) {
            event.setKickMessage(Config.KICK_MESSAGE);
        }
    }

    protected void onPreConnect(PQServerPreConnectEvent event) {
        PlayerWrapper player = event.getPlayer();
        QueueType type = QueueType.getQueueType(player::hasPermission);

        if (Config.AUTH_FIRST) {
            if (Config.ALWAYS_QUEUE)
                return;

            if (isAnyoneQueuedOfType(type))
                return;

            Optional<String> optionalTarget = event.getTarget();
            if (!isMainFull(type) && optionalTarget.isPresent() && optionalTarget.get().equals(Config.QUEUE_SERVER)) {
                event.setTarget(Config.MAIN_SERVER);
            }
        } else {
            if (player.getCurrentServer().isPresent())
                return;

            if ((Config.KICK_WHEN_DOWN && !mainOnline) || !queueOnline || !authOnline) { // authOnline is always true if auth is not enabled
                player.disconnect(Config.SERVER_DOWN_KICK_MESSAGE);
                return;
            }

            if (Config.ALWAYS_QUEUE || isServerFull(type)) {
                if (player.hasPermission(Config.QUEUE_BYPASS_PERMISSION)) {
                    event.setTarget(Config.MAIN_SERVER);
                } else {
                    putQueue(player, event);
                }
            }
        }
    }

    private void putQueue(PlayerWrapper player, PQServerPreConnectEvent event) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type);

        // Redirect the player to the queue.
        Optional<String> originalTarget = event.getTarget();

        event.setTarget(Config.QUEUE_SERVER);

        Map<UUID, String> queueMap = type.getQueueMap();

        // Store the data concerning the player's original destination
        if (Config.FORCE_MAIN_SERVER || !originalTarget.isPresent()) {
            queueMap.put(player.getUniqueId(), Config.MAIN_SERVER);
        } else {
            queueMap.put(player.getUniqueId(), originalTarget.get());
        }
    }

    protected void onConnected(PQServerConnectedEvent event) {
        PlayerWrapper player = event.getPlayer();

        if (Config.AUTH_FIRST) {
            if (isAuthToQueue(event) && player.hasPermission(Config.QUEUE_BYPASS_PERMISSION)) {
                player.connect(Config.MAIN_SERVER);
                return;
            }

            // It's not present when joining!
            Optional<String> currentServer = player.getCurrentServer();
            if (!event.getPreviousServer().isPresent() && currentServer.isPresent() && currentServer.get().equals(Config.QUEUE_SERVER)) {
                if (Config.ALLOW_AUTH_SKIP)
                    putQueueAuthFirst(player);
            } else if (isAuthToQueue(event)) {
                putQueueAuthFirst(player);
            }
        }
    }

    public void putQueueAuthFirst(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type);

        // Store the data concerning the player's original destination
        type.getQueueMap().put(player.getUniqueId(), Config.MAIN_SERVER);
    }

    private void preQueueAdding(PlayerWrapper player, QueueType type) {
        player.sendPlayerListHeaderAndFooter(type.getHeader(), type.getFooter());

        if (isServerFull(type)) {
            player.sendMessage(Config.SERVER_IS_FULL_MESSAGE);
        }
    }

    private boolean isServerFull(QueueType type) {
        return isMainFull(type) || isAnyoneQueuedOfType(type);
    }

    private int getFreeSlots(QueueType type) {
        return type.getReservedSlots() - type.getPlayersWithTypeInMain().get();
    }

    private boolean isMainFull(QueueType type) {
        return getFreeSlots(type) <= 0;
    }

    private boolean isAnyoneQueuedOfType(QueueType type) {
        return !type.getQueueMap().isEmpty();
    }

    private boolean isAuthToQueue(PQServerConnectedEvent event) {
        Optional<String> previousServer = event.getPreviousServer();
        return previousServer.isPresent() && previousServer.get().equals(Config.AUTH_SERVER) && event.getServer().equals(Config.QUEUE_SERVER);
    }

    public void moveQueue() {
        for (QueueType type : Config.QUEUE_TYPES) {
            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
                Optional<PlayerWrapper> player = plugin.getPlayer(entry.getKey());

                Optional<String> optionalTarget = player.flatMap(PlayerWrapper::getCurrentServer);
                if (!optionalTarget.isPresent() || !optionalTarget.get().equals(Config.QUEUE_SERVER)) {
                    type.getQueueMap().remove(entry.getKey());
                }
            }
        }

        if (Config.RECOVERY) {
            plugin.getPlayers().forEach(this::doRecovery);
        }

        if (Config.PAUSE_QUEUE_IF_MAIN_DOWN) {
            if (mainOnline) {
                if (onlineSince != null) {
                    if (Duration.between(onlineSince, Instant.now()).getSeconds() >= Config.START_TIME) {
                        onlineSince = null;
                    } else {
                        return;
                    }
                }
            } else {
                return;
            }
        }

        Arrays.stream(Config.QUEUE_TYPES).forEachOrdered(this::connectPlayer);
    }

    private void doRecovery(PlayerWrapper player) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        Optional<String> currentServer = player.getCurrentServer();
        if (!type.getQueueMap().containsKey(player.getUniqueId()) && currentServer.isPresent() && currentServer.get().equals(Config.QUEUE_SERVER)) {
            type.getQueueMap().putIfAbsent(player.getUniqueId(), Config.MAIN_SERVER);

            player.sendMessage(Config.RECOVERY_MESSAGE);
        }
    }

    private void connectPlayer(QueueType type) {
        int freeSlots = getFreeSlots(type);

        if (freeSlots <= 0)
            return;

        if (freeSlots > Config.MAX_PLAYERS_PER_MOVE)
            freeSlots = Config.MAX_PLAYERS_PER_MOVE;

        for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(type.getQueueMap()).entrySet()) {
            Optional<PlayerWrapper> optional = plugin.getPlayer(entry.getKey());
            if (!optional.isPresent()) {
                continue;
            }
            PlayerWrapper player = optional.get();

            type.getQueueMap().remove(entry.getKey());

            player.sendMessage(Config.JOINING_MAIN_SERVER);
            player.sendPlayerListHeaderAndFooter(null, null);

            if (StorageTool.isShadowBanned(player.getUniqueId())
                    && (Config.SHADOW_BAN_TYPE == BanType.LOOP
                    || (Config.SHADOW_BAN_TYPE == BanType.TEN_PERCENT && ThreadLocalRandom.current().nextInt(100) >= 10)
                    || (Config.SHADOW_BAN_TYPE == BanType.CUSTOM_PERCENT && ThreadLocalRandom.current().nextInt(100) >= Config.CUSTOM_PERCENT_PERCENTAGE))) {
                player.sendMessage(Config.SHADOW_BAN_MESSAGE);

                type.getQueueMap().put(entry.getKey(), entry.getValue());

                continue;
            }

            indexPositionTime(type);

            Map<Integer, Instant> cache = type.getPositionCache().get(entry.getKey());
            if (cache != null) {
                cache.forEach((position, instant) ->
                        type.getDurationToPosition().put(position, Duration.between(instant, Instant.now())));
            }

            player.connect(entry.getValue());

            if (--freeSlots <= 0)
                break;
        }

        if (Config.SEND_XP_SOUND) {
            sendXPSoundToQueueType(type);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void sendXPSoundToQueueType(QueueType type) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("xp");

        int counter = 0;
        for (UUID uuid : type.getQueueMap().keySet()) {
            if (++counter > 5) {
                break;
            }

            out.writeUTF(uuid.toString());
        }

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
