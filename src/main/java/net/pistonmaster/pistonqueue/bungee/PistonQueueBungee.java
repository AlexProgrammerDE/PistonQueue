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
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.pistonmaster.pistonqueue.bungee.commands.MainCommand;
import net.pistonmaster.pistonqueue.bungee.listeners.PistonListener;
import net.pistonmaster.pistonqueue.bungee.listeners.QueueListenerBungee;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.bungee.utils.StorageTool;
import net.pistonmaster.pistonqueue.hooks.PistonMOTDPlaceholder;
import net.pistonmaster.pistonqueue.shared.*;
import org.bstats.bungeecord.Metrics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@SuppressWarnings({"deprecation"})
public final class PistonQueueBungee extends Plugin implements PistonQueueProxy {
    @Getter
    private final QueueListenerBungee queueListenerBungee = new QueueListenerBungee(this);

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        PluginManager manager = getProxy().getPluginManager();

        logger.info(ChatColor.BLUE + "Loading config");
        processConfig(getDataFolder());

        StorageTool.setupTool(this);
        initializeReservationSlots();

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
        manager.registerListener(this, queueListenerBungee);
        manager.registerListener(this, new PistonListener(this));

        logger.info(ChatColor.BLUE + "Loading Metrics");
        new Metrics(this, 8755);

        logger.info(ChatColor.BLUE + "Checking for update");
        new UpdateChecker(getLogger()::info, 83541).getVersion(version -> {
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
            if (Config.PAUSEQUEUEIFMAINDOWN && !queueListenerBungee.isMainOnline()) {
                QueueType.VETERAN.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        ChatUtils.sendMessage(player, Config.PAUSEQUEUEIFMAINDOWNMESSAGE);
                });

                QueueType.PRIORITY.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        ChatUtils.sendMessage(player, Config.PAUSEQUEUEIFMAINDOWNMESSAGE);
                });

                QueueType.REGULAR.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        ChatUtils.sendMessage(player, Config.PAUSEQUEUEIFMAINDOWNMESSAGE);
                });
            }
        }, Config.POSITIONMESSAGEDELAY, Config.POSITIONMESSAGEDELAY, TimeUnit.MILLISECONDS);

        // Send plugin message
        getProxy().getScheduler().schedule(this, this::sendCustomData, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Moves the queue when someone logs off the main server on an interval set in the config.yml
        getProxy().getScheduler().schedule(this, queueListenerBungee::moveQueue, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Checks the status of all the servers
        getProxy().getScheduler().schedule(this, () -> {
            if (getProxy().getServers().containsKey(Config.MAINSERVER)) {
                try {
                    Socket s = new Socket(
                            getProxy().getServerInfo(Config.MAINSERVER).getAddress().getAddress(),
                            getProxy().getServerInfo(Config.MAINSERVER).getAddress().getPort());

                    s.close();

                    if (!queueListenerBungee.isMainOnline())
                        queueListenerBungee.setOnlineSince(Instant.now());

                    queueListenerBungee.setMainOnline(true);
                } catch (IOException e) {
                    getLogger().warning("Main Server is down!!!");
                    queueListenerBungee.setMainOnline(false);
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
                    queueListenerBungee.setQueueOnline(true);
                } catch (IOException e) {
                    getLogger().warning("Queue Server is down!!!");
                    queueListenerBungee.setQueueOnline(false);
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
                        queueListenerBungee.setAuthOnline(true);
                    } catch (IOException e) {
                        getLogger().warning("Auth Server is down!!!");
                        queueListenerBungee.setAuthOnline(false);
                    }
                } else {
                    getLogger().warning("Auth Server \"" + Config.AUTHSERVER + "\" not set up!!!");
                }
            } else {
                queueListenerBungee.setAuthOnline(true);
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);
    }

    private void sendMessage(QueueType queue, boolean bool, ChatMessageType type) {
        if (bool) {
            if (!queueListenerBungee.isMainOnline())
                return;

            int position = 0;

            for (Entry<UUID, String> entry : new LinkedHashMap<>(queue.getQueueMap()).entrySet()) {
                ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                if (player == null || !player.isConnected()) {
                    continue;
                }

                position++;

                ChatUtils.sendMessage(type, player, Config.QUEUEPOSITION
                        .replace("%position%", String.valueOf(position))
                        .replace("%total%", String.valueOf(queue.getQueueMap().size()))
                );
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

    private void initializeReservationSlots() {
        getProxy().getScheduler().schedule(this, () -> {
            ServerInfo mainServer = getProxy().getServerInfo(Config.MAINSERVER);
            Map<QueueType, AtomicInteger> map = new EnumMap<>(QueueType.class);

            for (ProxiedPlayer player : mainServer.getPlayers()) {
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
        }, 0, 1, TimeUnit.SECONDS);
    }

    public PlayerWrapper wrapPlayer(ProxiedPlayer player) {
        return new PlayerWrapper() {
            @Override
            public boolean hasPermission(String node) {
                return player.hasPermission(node);
            }

            @Override
            public void connect(String server) {
                Optional<ServerInfo> optional = Optional.ofNullable(getProxy().getServerInfo(server));

                if (!optional.isPresent()) {
                    getLogger().severe("Server" + server + " not found!!!");
                    return;
                }

                player.connect(optional.get());
            }

            @Override
            public Optional<String> getCurrentServer() {
                return Optional.ofNullable(player.getServer()).map(server -> server.getInfo().getName());
            }

            @Override
            public void sendMessage(String message) {
                ChatUtils.sendMessage(ChatMessageType.CHAT, player, message);
            }

            @Override
            public void sendActionBar(String message) {
                ChatUtils.sendMessage(ChatMessageType.ACTION_BAR, player, message);
            }

            @Override
            public void sendPlayerListHeaderAndFooter(List<String> header, List<String> footer) {
                player.setTabHeader(ChatUtils.parseTab(header), ChatUtils.parseTab(footer));
            }

            @Override
            public UUID getUniqueId() {
                return player.getUniqueId();
            }
        };
    }
}
