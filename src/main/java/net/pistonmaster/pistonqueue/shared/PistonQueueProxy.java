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
import net.pistonmaster.pistonqueue.shared.utils.ConfigOutdatedException;
import net.pistonmaster.pistonqueue.shared.utils.MessageType;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public interface PistonQueueProxy {
    Optional<PlayerWrapper> getPlayer(UUID uuid);

    List<PlayerWrapper> getPlayers();

    Optional<ServerInfoWrapper> getServer(String name);

    void schedule(Runnable runnable, long delay, long period, TimeUnit unit);

    void info(String message);

    void warning(String message);

    void error(String message);

    default void sendMessage(QueueType queue, boolean bool, MessageType type) {
        if (bool) {
            int position = 0;

            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
                Optional<PlayerWrapper> player = getPlayer(entry.getKey());
                if (!player.isPresent()) {
                    continue;
                }

                position++;

                player.get().sendMessage(type, Config.QUEUE_POSITION
                        .replace("%position%", String.valueOf(position))
                        .replace("%total%", String.valueOf(queue.getQueueMap().size())));
            }
        }
    }

    default void updateTab(QueueType queue, List<String> header, List<String> footer) {
        int position = 0;

        for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
            Optional<PlayerWrapper> player = getPlayer(entry.getKey());
            if (!player.isPresent()) {
                continue;
            }

            position++;

            int finalPosition = position;
            header = header.stream().map(str -> replacePosition(str, finalPosition, queue)).collect(Collectors.toList());
            footer = footer.stream().map(str -> replacePosition(str, finalPosition, queue)).collect(Collectors.toList());

            player.get().sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    default String replacePosition(String text, int position, QueueType type) {
        if (type.getDurationToPosition().containsKey(position)) {
            Duration duration = type.getDurationToPosition().get(position);

            return SharedChatUtils.formatDuration(text, duration, position);
        } else {
            AtomicInteger biggestPositionAtomic = new AtomicInteger();
            AtomicReference<Duration> bestDurationAtomic = new AtomicReference<>(Duration.ZERO);

            type.getDurationToPosition().forEach((integer, instant) -> {
                if (integer > biggestPositionAtomic.get()) {
                    biggestPositionAtomic.set(integer);
                    bestDurationAtomic.set(instant);
                }
            });

            int biggestPosition = biggestPositionAtomic.get();
            Duration biggestDuration = bestDurationAtomic.get();

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

            Map<QueueType, AtomicInteger> map = new EnumMap<>(QueueType.class);

            for (PlayerWrapper player : mainServer.get().getConnectedPlayers()) {
                QueueType playerType = QueueType.getQueueType(player::hasPermission);

                if (map.containsKey(playerType)) {
                    map.get(playerType).incrementAndGet();
                } else {
                    map.put(playerType, new AtomicInteger(1));
                }
            }

            for (Map.Entry<QueueType, AtomicInteger> entry : map.entrySet()) {
                entry.getKey().getPlayersWithTypeInMain().set(entry.getValue().get());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @SuppressWarnings({"UnstableApiUsage"})
    default void sendCustomData() {
        List<PlayerWrapper> networkPlayers = getPlayers();

        if (networkPlayers == null || networkPlayers.isEmpty()) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("size");
        out.writeInt(QueueType.REGULAR.getQueueMap().size());
        out.writeInt(QueueType.PRIORITY.getQueueMap().size());
        out.writeInt(QueueType.VETERAN.getQueueMap().size());

        networkPlayers.forEach(player -> {
            if (player.getCurrentServer().isPresent()) {
                getServer(player.getCurrentServer().get()).ifPresent(serverInfoWrapper ->
                        serverInfoWrapper.sendPluginMessage("piston:queue", out.toByteArray()));
            }
        });
    }

    default void processConfig(File dataDirectory) {
        try {
            if (!dataDirectory.exists() && !dataDirectory.mkdir())
                return;

            File file = new File(dataDirectory, "config.yml");

            if (!file.exists()) {
                try {
                    Files.copy(Objects.requireNonNull(PistonQueueVelocity.class.getClassLoader().getResourceAsStream("proxyconfig.yml")), file.toPath());
                    loadConfig(dataDirectory);
                    return;
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }

            loadConfig(dataDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    default void loadConfig(File dataDirectory) throws IOException {
        ConfigurationNode config = YamlConfigurationLoader.builder().path(new File(dataDirectory, "config.yml").toPath()).build().load();

        Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);

                String fieldName = it.getName().replace("_", "");
                if (List.class.isAssignableFrom(it.getType())) {
                    it.set(Config.class, config.node(fieldName).getList(String.class));
                } else {
                    it.set(Config.class, config.node(fieldName).get(it.getType()));
                }
            } catch (SecurityException | IllegalAccessException | SerializationException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                String[] text = e.getMessage().split(" ");
                String value = "";

                for (String str : text) {
                    if (str.toLowerCase().startsWith(PistonQueueVelocity.class.getPackage().getName().toLowerCase())) {
                        value = str;
                    }
                }

                String[] packageSplit = value.split("\\.");

                new ConfigOutdatedException(packageSplit[packageSplit.length - 1]).printStackTrace();
            }
        });
    }
}
