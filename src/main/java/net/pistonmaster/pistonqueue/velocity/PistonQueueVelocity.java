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
package net.pistonmaster.pistonqueue.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.pistonmaster.pistonqueue.data.PluginData;
import net.pistonmaster.pistonqueue.hooks.PistonMOTDPlaceholder;
import net.pistonmaster.pistonqueue.shared.utils.*;
import net.pistonmaster.pistonqueue.velocity.commands.MainCommand;
import net.pistonmaster.pistonqueue.velocity.listeners.PistonListener;
import net.pistonmaster.pistonqueue.velocity.listeners.QueueListener;
import net.pistonmaster.pistonqueue.velocity.utils.ChatUtils;
import net.pistonmaster.pistonqueue.velocity.utils.MessageType;
import net.pistonmaster.pistonqueue.velocity.utils.StorageTool;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Plugin(id = "pistonqueue", name = PluginData.NAME, version = PluginData.VERSION,
        url = PluginData.URL, description = PluginData.DESCRIPTION, authors = {"AlexProgrammerDE"})
public class PistonQueueVelocity {
    @Getter
    private final File dataDirectory;
    @Getter
    private final ProxyServer server;
    @Getter
    private final Logger logger;
    @Getter
    private final PluginContainer pluginContainer;
    @Getter
    private final QueueListener queueListener = new QueueListener(this);
    private final Metrics.Factory metricsFactory;
    @Getter
    private BanType banType;

    @Inject
    public PistonQueueVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, PluginContainer pluginContainer, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory.toFile();
        this.pluginContainer = pluginContainer;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Loading config");
        processConfig();

        StorageTool.setupTool(this);
        initializeReservationSlots();

        logger.info("Looking for hooks");
        if (server.getPluginManager().getPlugin("pistonmotd").isPresent()) {
            logger.info("Hooking into PistonMOTD");
            new PistonMOTDPlaceholder();
        }

        logger.info("Registering plugin messaging channel");
        server.getChannelRegistrar().register(MinecraftChannelIdentifier.from("piston:queue"));

        logger.info("Registering commands");
        server.getCommandManager().register("pistonqueue", new MainCommand(this), "pq");

        logger.info("Registering listeners");
        server.getEventManager().register(this, new PistonListener(this));
        server.getEventManager().register(this, queueListener);

        logger.info("Loading Metrics");
        metricsFactory.make(this, 12389);

        logger.info("Checking for update");
        new UpdateChecker(logger::info, 83541).getVersion(version -> {
            if (pluginContainer.getDescription().getVersion().orElse("unknown").equalsIgnoreCase(version)) {
                logger.info("Your up to date!");
            } else {
                logger.info("There is a update available.");
                logger.info("Current version: " + pluginContainer.getDescription().getVersion().orElse("unknown") + " New version: " + version);
                logger.info("Download it at: https://www.spigotmc.org/resources/83541");
            }
        });

        logger.info("Scheduling tasks");

        // Sends the position message and updates tab on an interval in chat
        server.getScheduler().buildTask(this, () -> {
            for (QueueType type : QueueType.values()) {
                sendMessage(type, Config.POSITIONMESSAGECHAT, MessageType.CHAT);
            }
        }).delay(Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS).repeat(Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS).schedule();

        // Sends the position message and updates tab on an interval on hotbar
        server.getScheduler().buildTask(this, () -> {
            for (QueueType type : QueueType.values()) {
                sendMessage(type, Config.POSITIONMESSAGEHOTBAR, MessageType.ACTION_BAR);
            }
        }).delay(Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS).repeat(Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS).schedule();

        // Updates the tab
        server.getScheduler().buildTask(this, () -> {
            updateTab(QueueType.VETERAN, Config.HEADERVETERAN, Config.FOOTERVETERAN);
            updateTab(QueueType.PRIORITY, Config.HEADERPRIORITY, Config.FOOTERPRIORITY);
            updateTab(QueueType.REGULAR, Config.HEADER, Config.FOOTER);
        }).delay(Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS).repeat(Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS).schedule();

        server.getScheduler().buildTask(this, () -> {
            if (Config.PAUSEQUEUEIFMAINDOWN && !queueListener.isMainOnline()) {
                QueueType.VETERAN.getQueueMap().forEach((UUID id, String str) -> server.getPlayer(id).ifPresent(value -> value.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE))));
                QueueType.PRIORITY.getQueueMap().forEach((UUID id, String str) -> server.getPlayer(id).ifPresent(value -> value.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE))));
                QueueType.REGULAR.getQueueMap().forEach((UUID id, String str) -> server.getPlayer(id).ifPresent(value -> value.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE))));
            }
        }).delay(Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS).repeat(Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS).schedule();

        // Send plugin message
        server.getScheduler().buildTask(this, this::sendCustomData).delay(Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS).repeat(Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS).schedule();

        // Moves the queue when someone logs off the main server on an interval set in the config.yml
        server.getScheduler().buildTask(this, queueListener::moveQueue).delay(Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS).repeat(Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS).schedule();

        // Checks the status of all the servers
        server.getScheduler().buildTask(this, () -> {
            if (server.getServer(Config.MAINSERVER).isPresent()) {
                try {
                    server.getServer(Config.MAINSERVER).get().ping().join();

                    if (!queueListener.isMainOnline())
                        queueListener.setOnlineSince(Instant.now());

                    queueListener.setMainOnline(true);
                } catch (CancellationException | CompletionException e) {
                    logger.warn("Main Server is down!!!");
                    queueListener.setMainOnline(false);
                }
            } else {
                logger.warn("Main Server \"" + Config.MAINSERVER + "\" not set up!!!");
            }
        }).delay(500, TimeUnit.MILLISECONDS).repeat(Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS).schedule();

        server.getScheduler().buildTask(this, () -> {
            if (server.getServer(Config.QUEUESERVER).isPresent()) {
                try {
                    server.getServer(Config.QUEUESERVER).get().ping().join();
                    queueListener.setQueueOnline(true);
                } catch (CancellationException | CompletionException e) {
                    logger.warn("Queue Server is down!!!");
                    queueListener.setQueueOnline(false);
                }
            } else {
                logger.warn("Queue Server \"" + Config.QUEUESERVER + "\" not set up!!!");
            }
        }).delay(500, TimeUnit.MILLISECONDS).repeat(Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS).schedule();

        server.getScheduler().buildTask(this, () -> {
            if (Config.ENABLEAUTHSERVER) {
                if (server.getServer(Config.AUTHSERVER).isPresent()) {
                    try {
                        server.getServer(Config.AUTHSERVER).get().ping().join();
                        queueListener.setAuthOnline(true);
                    } catch (CancellationException | CompletionException e) {
                        logger.warn("Auth Server is down!!!");
                        queueListener.setAuthOnline(false);
                    }
                } else {
                    logger.warn("Auth Server \"" + Config.AUTHSERVER + "\" not set up!!!");
                }
            } else {
                queueListener.setAuthOnline(true);
            }
        }).delay(500, TimeUnit.MILLISECONDS).repeat(Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS).schedule();
    }

    public void processConfig() {
        try {
            if (!dataDirectory.exists() && !dataDirectory.mkdir())
                return;

            File file = new File(dataDirectory, "config.yml");

            if (!file.exists()) {
                try {
                    Files.copy(Objects.requireNonNull(PistonQueueVelocity.class.getClassLoader().getResourceAsStream("proxyconfig.yml")), file.toPath());
                    loadConfig();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }

            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfig() throws IOException {
        ConfigurationNode config = YamlConfigurationLoader.builder().path(new File(dataDirectory, "config.yml").toPath()).build().load();

        Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);

                if (List.class.isAssignableFrom(it.getType())) {
                    it.set(Config.class, config.node(it.getName()).getList(String.class));
                } else {
                    it.set(Config.class, config.node(it.getName()).get(it.getType()));
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

        banType = BanType.valueOf(config.node("SHADOWBANTYPE").getString());
    }

    private void sendMessage(QueueType queue, boolean bool, MessageType type) {
        if (bool) {
            if (!queueListener.isMainOnline())
                return;

            int position = 0;

            for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
                Optional<Player> player = server.getPlayer(entry.getKey());
                if (!player.isPresent()) {
                    continue;
                }

                position++;

                switch (type) {
                    case CHAT:
                        player.get().sendMessage(ChatUtils.parseToComponent(Config.QUEUEPOSITION
                                .replace("%position%", String.valueOf(position))
                                .replace("%total%", String.valueOf(queue.getQueueMap().size()))));
                        break;
                    case ACTION_BAR:
                        player.get().sendActionBar(
                                ChatUtils.parseToComponent(Config.QUEUEPOSITION
                                        .replace("%position%", String.valueOf(position))
                                        .replace("%total%", String.valueOf(queue.getQueueMap().size()))));
                        break;
                }
            }
        }
    }

    private void updateTab(QueueType queue, List<String> header, List<String> footer) {
        int position = 0;

        for (Map.Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
            Optional<Player> player = server.getPlayer(entry.getKey());
            if (!player.isPresent()) {
                continue;
            }

            position++;

            StringBuilder headerBuilder = new StringBuilder();
            StringBuilder footerBuilder = new StringBuilder();

            for (int i = 0; i < header.size(); i++) {
                headerBuilder.append(ChatUtils.parseToString(replacePosition(header.get(i), position, queue)));

                if (i != (header.size() - 1)) {
                    headerBuilder.append("\n");
                }
            }

            for (int i = 0; i < footer.size(); i++) {
                footerBuilder.append(ChatUtils.parseToString(replacePosition(footer.get(i), position, queue)));

                if (i != (footer.size() - 1)) {
                    footerBuilder.append("\n");
                }
            }

            player.get().sendPlayerListHeaderAndFooter(
                    LegacyComponentSerializer.legacySection().deserialize(headerBuilder.toString()),
                    LegacyComponentSerializer.legacySection().deserialize(footerBuilder.toString()));
        }
    }

    private String replacePosition(String text, int position, QueueType type) {
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

    private void sendCustomData() {
        Collection<Player> networkPlayers = server.getAllPlayers();

        if (networkPlayers == null || networkPlayers.isEmpty()) {
            return;
        }

        @SuppressWarnings({"UnstableApiUsage"})
        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("size");
        out.writeInt(QueueType.REGULAR.getQueueMap().size());
        out.writeInt(QueueType.PRIORITY.getQueueMap().size());
        out.writeInt(QueueType.VETERAN.getQueueMap().size());

        networkPlayers.forEach(player -> {
            player.sendPluginMessage(() -> "piston:queue", out.toByteArray());
        });
    }

    private void initializeReservationSlots() {
        server.getScheduler().buildTask(this, () -> {
            if (!server.getServer(Config.MAINSERVER).isPresent())
                throw new IllegalStateException("Main server not configured properly!!!");

            RegisteredServer mainServer = server.getServer(Config.MAINSERVER).get();
            Map<QueueType, AtomicInteger> map = new EnumMap<>(QueueType.class);

            for (Player player : mainServer.getPlayersConnected()) {
                QueueType playerType = QueueType.getQueueType(player::hasPermission);

                if (map.containsKey(playerType)) {
                    map.get(playerType).incrementAndGet();
                } else {
                    map.put(playerType, new AtomicInteger(1));
                }
            }

            for (Map.Entry<QueueType, AtomicInteger> entry : map.entrySet()) {
                entry.getKey().setPlayersWithTypeInMain(entry.getValue().get());
            }
        }).delay(0, TimeUnit.MILLISECONDS).repeat(1, TimeUnit.SECONDS).schedule();
    }
}
