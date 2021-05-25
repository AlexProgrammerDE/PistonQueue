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
package net.pistonmaster.pistonqueue.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.pistonmaster.pistonqueue.bungee.commands.MainCommand;
import net.pistonmaster.pistonqueue.bungee.hooks.PistonMOTDPlaceholder;
import net.pistonmaster.pistonqueue.bungee.listeners.PistonListener;
import net.pistonmaster.pistonqueue.bungee.listeners.QueueListener;
import net.pistonmaster.pistonqueue.bungee.utils.*;
import org.bstats.bungeecord.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

@SuppressWarnings({"deprecation"})
public final class PistonQueue extends Plugin {
    @Getter
    private final QueueListener queueListener = new QueueListener(this);
    @Getter
    private BanType banType;

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        PluginManager manager = getProxy().getPluginManager();

        logger.info(ChatColor.BLUE + "Loading config");
        processConfig();

        StorageTool.setupTool(this);
        QueueType.initializeReservationSlots(this);

        logger.info(ChatColor.BLUE + "Looking for hooks");
        if (getProxy().getPluginManager().getPlugin("PistonMOTD") != null) {
            logger.info(ChatColor.BLUE + "Hooking into PistonMOTD");
            new PistonMOTDPlaceholder();
        }

        logger.info(ChatColor.BLUE + "Registering plugin messaging channel");
        getProxy().registerChannel("piston:queue");

        logger.info(ChatColor.BLUE + "Registering commands");
        manager.registerCommand(this, new MainCommand(this));

        logger.info(ChatColor.BLUE + "Registering listeners");
        manager.registerListener(this, queueListener);
        manager.registerListener(this, new PistonListener(this));

        logger.info(ChatColor.BLUE + "Loading Metrics");
        new Metrics(this, 8755);

        logger.info(ChatColor.BLUE + "Checking for update");
        new UpdateChecker(this, 83541).getVersion(version -> {
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                logger.info(ChatColor.BLUE + "Your up to date!");
            } else {
                logger.info(ChatColor.RED + "There is a update available.");
                logger.info(ChatColor.RED + "Current version: " + this.getDescription().getVersion() + " New version: " + version);
                logger.info(ChatColor.RED + "Download it at: https://www.spigotmc.org/resources/83541");
            }
        });

        logger.info(ChatColor.BLUE + "Scheduling tasks");

        // Sends the position message and updates tab on an interval in chat
        getProxy().getScheduler().schedule(this, () -> {
            for (QueueType type : QueueType.values()) {
                sendMessage(type, Config.POSITIONMESSAGECHAT, ChatMessageType.CHAT);
            }
        }, Config.POSITIONMESSAGEDELAY, Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS);

        // Sends the position message and updates tab on an interval on hotbar
        getProxy().getScheduler().schedule(this, () -> {
            for (QueueType type : QueueType.values()) {
                sendMessage(type, Config.POSITIONMESSAGEHOTBAR, ChatMessageType.ACTION_BAR);
            }
        }, Config.POSITIONMESSAGEDELAY, Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS);

        // Updates the tab
        getProxy().getScheduler().schedule(this, () -> {
            updateTab(QueueType.VETERAN, Config.HEADERVETERAN, Config.FOOTERVETERAN);
            updateTab(QueueType.PRIORITY, Config.HEADERPRIORITY, Config.FOOTERPRIORITY);
            updateTab(QueueType.REGULAR, Config.HEADER, Config.FOOTER);
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (Config.PAUSEQUEUEIFMAINDOWN && !queueListener.isMainOnline()) {
                QueueType.VETERAN.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
                });

                QueueType.PRIORITY.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
                });

                QueueType.REGULAR.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        player.sendMessage(ChatUtils.parseToComponent(Config.PAUSEQUEUEIFMAINDOWNMESSAGE));
                });
            }
        }, Config.POSITIONMESSAGEDELAY, Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS);

        // Send plugin message
        getProxy().getScheduler().schedule(this, this::sendCustomData, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Moves the queue when someone logs off the main server on an interval set in the config.yml
        getProxy().getScheduler().schedule(this, queueListener::moveQueue, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Checks the status of all the servers
        getProxy().getScheduler().schedule(this, () -> {
            if (getProxy().getServers().containsKey(Config.MAINSERVER)) {
                try {
                    Socket s = new Socket(
                            getProxy().getServerInfo(Config.MAINSERVER).getAddress().getAddress(),
                            getProxy().getServerInfo(Config.MAINSERVER).getAddress().getPort());

                    s.close();

                    if (!queueListener.isMainOnline())
                        queueListener.setOnlineSince(Instant.now());

                    queueListener.setMainOnline(true);
                } catch (IOException e) {
                    getLogger().warning("Main Server is down!!!");
                    queueListener.setMainOnline(false);
                }
            } else {
                getLogger().warning("Main Server \"" + Config.MAINSERVER + "\" not set up!!!");
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (getProxy().getServers().containsKey(Config.QUEUESERVER)) {
                try {
                    Socket s = new Socket(
                            getProxy().getServerInfo(Config.QUEUESERVER).getAddress().getAddress(),
                            getProxy().getServerInfo(Config.QUEUESERVER).getAddress().getPort());

                    s.close();
                    queueListener.setQueueOnline(true);
                } catch (IOException e) {
                    getLogger().warning("Queue Server is down!!!");
                    queueListener.setQueueOnline(false);
                }
            } else {
                getLogger().warning("Queue Server \"" + Config.QUEUESERVER + "\" not set up!!!");
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (Config.ENABLEAUTHSERVER) {
                if (getProxy().getServers().containsKey(Config.AUTHSERVER)) {
                    try {
                        Socket s = new Socket(
                                getProxy().getServerInfo(Config.AUTHSERVER).getAddress().getAddress(),
                                getProxy().getServerInfo(Config.AUTHSERVER).getAddress().getPort());

                        s.close();
                        queueListener.setAuthOnline(true);
                    } catch (IOException e) {
                        getLogger().warning("Auth Server is down!!!");
                        queueListener.setAuthOnline(false);
                    }
                } else {
                    getLogger().warning("Auth Server \"" + Config.AUTHSERVER + "\" not set up!!!");
                }
            } else {
                queueListener.setAuthOnline(true);
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);
    }

    public void processConfig() {
        try {
            loadConfig();
        } catch (IOException e) {
            if (!getDataFolder().exists() && !getDataFolder().mkdir())
                return;

            File file = new File(getDataFolder(), "config.yml");

            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("bungeeconfig.yml")) {
                    Files.copy(in, file.toPath());
                    loadConfig();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    private void loadConfig() throws IOException {
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);
                it.set(Config.class, config.get(it.getName()));
            } catch (SecurityException | IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                String[] text = e.getMessage().split(" ");
                String value = "";

                for (String str : text) {
                    if (str.toLowerCase().startsWith(PistonQueue.class.getPackage().getName().toLowerCase())) {
                        value = str;
                    }
                }

                String[] packageSplit = value.split("\\.");

                new ConfigOutdatedException(packageSplit[packageSplit.length - 1]).printStackTrace();
            }
        });

        banType = BanType.valueOf(config.getString("SHADOWBANTYPE"));
    }

    private void sendMessage(QueueType queue, boolean bool, ChatMessageType type) {
        if (bool) {
            if (!queueListener.isMainOnline())
                return;

            int position = 0;

            for (Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
                ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                if (player == null || !player.isConnected()) {
                    continue;
                }

                position++;

                player.sendMessage(type,
                        ChatUtils.parseToComponent(Config.QUEUEPOSITION
                                .replace("%position%", String.valueOf(position))
                                .replace("%total%", String.valueOf(queue.getQueueMap().size()))
                                .replace("%server%", entry.getValue())));
            }
        }
    }

    private void updateTab(QueueType queue, List<String> header, List<String> footer) {
        int position = 0;

        for (Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
            ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
            if (player == null || !player.isConnected()) {
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

            player.setTabHeader(
                    new ComponentBuilder(headerBuilder.toString()).create(),
                    new ComponentBuilder(footerBuilder.toString()).create());
        }
    }

    private String replacePosition(String text, int position, QueueType type) {
        if (type.getDurationToPosition().containsKey(position)) {
            Duration duration = type.getDurationToPosition().get(position);

            return ChatUtils.formatDuration(text, duration, position);
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

            return ChatUtils.formatDuration(text, imaginaryDuration, position);
        }
    }

    private void sendCustomData() {
        Collection<ProxiedPlayer> networkPlayers = getProxy().getPlayers();

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
            if (player.getServer() != null)
                player.getServer().getInfo().sendData("piston:queue", out.toByteArray());
        });
    }
}
