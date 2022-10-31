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
import lombok.val;
import net.pistonmaster.pistonqueue.shared.utils.ConfigOutdatedException;
import net.pistonmaster.pistonqueue.shared.utils.MessageType;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public interface PistonQueuePlugin {
    Optional<PlayerWrapper> getPlayer(UUID uuid);

    Optional<PlayerWrapper> getPlayer(String name);

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
            if (!queueListener.isMainOnline())
                return;

            for (QueueType type : Config.QUEUE_TYPES) {
                sendMessage(type, Config.POSITION_MESSAGE_CHAT, MessageType.CHAT);
            }
        }, Config.POSITION_MESSAGE_DELAY, Config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

        // Sends the position message and updates tab on an interval on hot bar
        schedule(() -> {
            if (!queueListener.isMainOnline())
                return;

            for (QueueType type : Config.QUEUE_TYPES) {
                sendMessage(type, Config.POSITION_MESSAGE_HOT_BAR, MessageType.ACTION_BAR);
            }
        }, Config.POSITION_MESSAGE_DELAY, Config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

        // Updates the tab
        schedule(() -> {
            for (QueueType type : Config.QUEUE_TYPES) {
                updateTab(type);
            }
        }, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

        schedule(() -> {
            if (!Config.PAUSE_QUEUE_IF_MAIN_DOWN || queueListener.isMainOnline()) {
                return;
            }

            for (QueueType type : Config.QUEUE_TYPES) {
                type.getQueueMap().forEach((UUID id, String str) ->
                        getPlayer(id).ifPresent(value -> value.sendMessage(Config.PAUSE_QUEUE_IF_MAIN_DOWN_MESSAGE)));
            }
        }, Config.POSITION_MESSAGE_DELAY, Config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

        // Send plugin message
        schedule(this::sendCustomData, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

        // Moves the queue when someone logs off the main server on an interval set in the config.yml
        schedule(queueListener::moveQueue, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

        String message = "%s \"%s\" not set up!!! Check out: https://github.com/AlexProgrammerDE/PistonQueue/wiki/FAQ#server-not-set-up";
        AtomicBoolean isFirstRun = new AtomicBoolean(true);
        // Checks the status of all the servers
        schedule(() -> {
            Optional<ServerInfoWrapper> serverInfoWrapper = getServer(Config.MAIN_SERVER);

            if (serverInfoWrapper.isPresent()) {
                if (serverInfoWrapper.get().isOnline()) {
                    if (!isFirstRun.get() && !queueListener.isMainOnline()) {
                        queueListener.setOnlineSince(Instant.now());
                    }

                    queueListener.setMainOnline(true);
                } else {
                    warning("Main Server is down!!!");
                    queueListener.setMainOnline(false);
                }
                isFirstRun.set(false);
            } else {
                warning(String.format(message, "Main Server", Config.MAIN_SERVER));
            }
        }, 500, Config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);

        schedule(() -> {
            Optional<ServerInfoWrapper> serverInfoWrapper = getServer(Config.QUEUE_SERVER);

            if (serverInfoWrapper.isPresent()) {
                if (serverInfoWrapper.get().isOnline()) {
                    queueListener.setQueueOnline(true);
                } else {
                    warning("Queue Server is down!!!");
                    queueListener.setQueueOnline(false);
                }
            } else {
                warning(String.format(message, "Queue Server", Config.QUEUE_SERVER));
            }
        }, 500, Config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);

        schedule(() -> {
            if (Config.ENABLE_AUTH_SERVER) {
                Optional<ServerInfoWrapper> serverInfoWrapper = getServer(Config.AUTH_SERVER);

                if (serverInfoWrapper.isPresent()) {
                    if (serverInfoWrapper.get().isOnline()) {
                        queueListener.setAuthOnline(true);
                    } else {
                        warning("Auth Server is down!!!");
                        queueListener.setAuthOnline(false);
                    }
                } else {
                    warning(String.format(message, "Auth Server", Config.AUTH_SERVER));
                }
            } else {
                queueListener.setAuthOnline(true);
            }
        }, 500, Config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);
    }

    default void sendMessage(QueueType queue, boolean bool, MessageType type) {
        if (bool) {
            AtomicInteger position = new AtomicInteger();

            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
                getPlayer(entry.getKey()).ifPresent(player ->
                        player.sendMessage(type, Config.QUEUE_POSITION
                                .replace("%position%", String.valueOf(position.incrementAndGet()))
                                .replace("%total%", String.valueOf(queue.getQueueMap().size()))));
            }
        }
    }

    default void updateTab(QueueType queue) {
        AtomicInteger position = new AtomicInteger();

        for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
            getPlayer(entry.getKey()).ifPresent(player -> {
                int incrementedPosition = position.incrementAndGet();

                player.sendPlayerListHeaderAndFooter(
                        queue.getHeader().stream().map(str -> replacePosition(str, incrementedPosition, queue)).collect(Collectors.toList()),
                        queue.getFooter().stream().map(str -> replacePosition(str, incrementedPosition, queue)).collect(Collectors.toList()));
            });
        }
    }

    default String replacePosition(String text, int position, QueueType type) {
        if (type.getDurationToPosition().containsKey(position)) {
            Duration duration = type.getDurationToPosition().get(position);

            return SharedChatUtils.formatDuration(text, duration, position);
        } else {
            int biggestPosition = 0;
            Duration biggestDuration = Duration.ZERO;

            for (Map.Entry<Integer, Duration> entry : type.getDurationToPosition().entrySet()) {
                int positionOfDuration = entry.getKey();
                if (positionOfDuration > biggestPosition) {
                    biggestPosition = positionOfDuration;
                    biggestDuration = entry.getValue();
                }
            }

            int difference = position - biggestPosition;

            Duration imaginaryDuration = biggestDuration.plus(difference, ChronoUnit.MINUTES);

            return SharedChatUtils.formatDuration(text, imaginaryDuration, position);
        }
    }

    default void initializeReservationSlots() {
        schedule(() -> {
            Optional<ServerInfoWrapper> mainServer = getServer(Config.MAIN_SERVER);
            if (!mainServer.isPresent())
                return;

            Map<QueueType, AtomicInteger> map = new HashMap<>();

            for (PlayerWrapper player : mainServer.get().getConnectedPlayers()) {
                QueueType playerType = QueueType.getQueueType(player::hasPermission);

                if (map.containsKey(playerType)) {
                    map.get(playerType).incrementAndGet();
                } else {
                    map.put(playerType, new AtomicInteger(1));
                }
            }

            map.forEach((type, count) -> type.getPlayersWithTypeInMain().set(count.get()));
        }, 0, 1, TimeUnit.SECONDS);
    }

    @SuppressWarnings({"UnstableApiUsage"})
    default void sendCustomData() {
        List<PlayerWrapper> networkPlayers = getPlayers();

        if (networkPlayers == null || networkPlayers.isEmpty()) {
            return;
        }

        ByteArrayDataOutput outOnlineQueue = ByteStreams.newDataOutput();

        outOnlineQueue.writeUTF("onlineQueue");
        outOnlineQueue.writeInt(Config.QUEUE_TYPES.length);
        for (QueueType queueType : Config.QUEUE_TYPES) {
            outOnlineQueue.writeUTF(queueType.getName().toLowerCase());
            outOnlineQueue.writeInt(queueType.getQueueMap().size());
        }

        ByteArrayDataOutput outOnlineMain = ByteStreams.newDataOutput();

        outOnlineMain.writeUTF("onlineMain");
        outOnlineQueue.writeInt(Config.QUEUE_TYPES.length);
        for (QueueType queueType : Config.QUEUE_TYPES) {
            outOnlineQueue.writeInt(queueType.getPlayersWithTypeInMain().get());
        }

        Set<String> servers = new HashSet<>();
        networkPlayers.forEach(player -> player.getCurrentServer().ifPresent(servers::add));

        for (String server : servers) {
            getServer(server).ifPresent(serverInfoWrapper ->
                    serverInfoWrapper.sendPluginMessage("piston:queue", outOnlineQueue.toByteArray()));
            getServer(server).ifPresent(serverInfoWrapper ->
                    serverInfoWrapper.sendPluginMessage("piston:queue", outOnlineMain.toByteArray()));
        }
    }

    default void processConfig(Path dataDirectory) {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path file = dataDirectory.resolve("config.yml");

            if (!Files.exists(file)) {
                try {
                    Files.copy(Objects.requireNonNull(PistonQueuePlugin.class.getClassLoader().getResourceAsStream("proxy_config.yml")), file);
                    loadConfig(dataDirectory);
                    return;
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
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
                } else if (QueueType.class.isAssignableFrom(it.getType())) {
                    if (it.get(Config.class) == null) { // We will never replace on reload
                        val queueTypes = config.node("QUEUE_TYPES").childrenMap();
                        val array = new QueueType[queueTypes.size()];
                        int i = 0;
                        for (Map.Entry<Object, ? extends ConfigurationNode> entry : queueTypes.entrySet()) {
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
                    if (str.toLowerCase().startsWith(Config.class.getPackage().getName().toLowerCase())) {
                        value = str;
                    }
                }

                String[] packageSplit = value.split("\\.");

                new ConfigOutdatedException(packageSplit[packageSplit.length - 1]).printStackTrace();
            }
        });
    }
}
