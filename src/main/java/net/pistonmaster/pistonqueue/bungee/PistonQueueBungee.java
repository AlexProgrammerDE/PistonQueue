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

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.pistonmaster.pistonqueue.bungee.commands.MainCommand;
import net.pistonmaster.pistonqueue.bungee.listeners.PistonListener;
import net.pistonmaster.pistonqueue.bungee.listeners.QueueListenerBungee;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.hooks.PistonMOTDPlaceholder;
import net.pistonmaster.pistonqueue.shared.*;
import net.pistonmaster.pistonqueue.shared.utils.MessageType;
import net.pistonmaster.pistonqueue.shared.utils.UpdateChecker;
import org.bstats.bungeecord.Metrics;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class PistonQueueBungee extends Plugin implements PistonQueueProxy {
    @Getter
    private final QueueListenerBungee queueListenerBungee = new QueueListenerBungee(this);

    @Override
    public void onEnable() {
        PluginManager manager = getProxy().getPluginManager();

        info(ChatColor.BLUE + "Loading config");
        processConfig(getDataFolder());

        StorageTool.setupTool(getDataFolder());
        initializeReservationSlots();

        info(ChatColor.BLUE + "Looking for hooks");
        if (getProxy().getPluginManager().getPlugin("PistonMOTD") != null) {
            info(ChatColor.BLUE + "Hooking into PistonMOTD");
            new PistonMOTDPlaceholder();
        }

        info(ChatColor.BLUE + "Registering plugin messaging channel");
        getProxy().registerChannel("piston:queue");

        info(ChatColor.BLUE + "Registering commands");
        manager.registerCommand(this, new MainCommand(this));

        info(ChatColor.BLUE + "Registering listeners");
        manager.registerListener(this, queueListenerBungee);
        manager.registerListener(this, new PistonListener());

        info(ChatColor.BLUE + "Loading Metrics");
        new Metrics(this, 8755);

        info(ChatColor.BLUE + "Checking for update");
        new UpdateChecker(this::info, 83541).getVersion(version -> {
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                info(ChatColor.BLUE + "Your up to date!");
            } else {
                info(ChatColor.RED + "There is a update available.");
                info(ChatColor.RED + "Current version: " + this.getDescription().getVersion() + " New version: " + version);
                info(ChatColor.RED + "Download it at: https://www.spigotmc.org/resources/83541");
            }
        });

        info(ChatColor.BLUE + "Scheduling tasks");

        // Sends the position message and updates tab on an interval in chat
        schedule(() -> {
            if (!queueListenerBungee.isMainOnline())
                return;

            for (QueueType type : QueueType.values()) {
                sendMessage(type, Config.POSITION_MESSAGE_CHAT, MessageType.CHAT);
            }
        }, Config.POSITION_MESSAGE_DELAY, Config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

        // Sends the position message and updates tab on an interval on hot bar
        schedule(() -> {
            if (!queueListenerBungee.isMainOnline())
                return;

            for (QueueType type : QueueType.values()) {
                sendMessage(type, Config.POSITION_MESSAGE_HOT_BAR, MessageType.ACTION_BAR);
            }
        }, Config.POSITION_MESSAGE_DELAY, Config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

        // Updates the tab
        schedule(() -> {
            updateTab(QueueType.VETERAN, Config.HEADER_VETERAN, Config.FOOTER_VETERAN);
            updateTab(QueueType.PRIORITY, Config.HEADER_PRIORITY, Config.FOOTER_PRIORITY);
            updateTab(QueueType.REGULAR, Config.HEADER, Config.FOOTER);
        }, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

        schedule(() -> {
            if (Config.PAUSE_QUEUE_IF_MAIN_DOWN && !queueListenerBungee.isMainOnline()) {
                QueueType.VETERAN.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        ChatUtils.sendMessage(player, Config.PAUSE_QUEUE_IF_MAIN_DOWN_MESSAGE);
                });

                QueueType.PRIORITY.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        ChatUtils.sendMessage(player, Config.PAUSE_QUEUE_IF_MAIN_DOWN_MESSAGE);
                });

                QueueType.REGULAR.getQueueMap().forEach((UUID id, String str) -> {
                    ProxiedPlayer player = getProxy().getPlayer(id);

                    if (player != null && player.isConnected())
                        ChatUtils.sendMessage(player, Config.PAUSE_QUEUE_IF_MAIN_DOWN_MESSAGE);
                });
            }
        }, Config.POSITION_MESSAGE_DELAY, Config.POSITION_MESSAGE_DELAY, TimeUnit.MILLISECONDS);

        // Send plugin message
        schedule(this::sendCustomData, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

        // Moves the queue when someone logs off the main server on an interval set in the config.yml
        schedule(queueListenerBungee::moveQueue, Config.QUEUE_MOVE_DELAY, Config.QUEUE_MOVE_DELAY, TimeUnit.MILLISECONDS);

        AtomicBoolean isFirstRun = new AtomicBoolean(true);
        // Checks the status of all the servers
        schedule(() -> {
            Optional<ServerInfoWrapper> serverInfoWrapper = getServer(Config.MAIN_SERVER);

            if (serverInfoWrapper.isPresent()) {
                if (serverInfoWrapper.get().isOnline()) {
                    if (!isFirstRun.get() && !queueListenerBungee.isMainOnline()) {
                        queueListenerBungee.setOnlineSince(Instant.now());
                    }

                    queueListenerBungee.setMainOnline(true);
                } else {
                    warning("Main Server is down!!!");
                    queueListenerBungee.setMainOnline(false);
                }
                isFirstRun.set(false);
            } else {
                warning("Main Server \"" + Config.MAIN_SERVER + "\" not set up!!! Check out: https://github.com/AlexProgrammerDE/PistonQueue/wiki/FAQ#server-not-set-up");
            }
        }, 500, Config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);

        schedule(() -> {
            Optional<ServerInfoWrapper> serverInfoWrapper = getServer(Config.QUEUE_SERVER);

            if (serverInfoWrapper.isPresent()) {
                if (serverInfoWrapper.get().isOnline()) {
                    queueListenerBungee.setQueueOnline(true);
                } else {
                    warning("Queue Server is down!!!");
                    queueListenerBungee.setQueueOnline(false);
                }
            } else {
                warning("Queue Server \"" + Config.QUEUE_SERVER + "\" not set up!!! Check out: https://github.com/AlexProgrammerDE/PistonQueue/wiki/FAQ#server-not-set-up");
            }
        }, 500, Config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);

        schedule(() -> {
            if (Config.ENABLE_AUTH_SERVER) {
                Optional<ServerInfoWrapper> serverInfoWrapper = getServer(Config.AUTH_SERVER);

                if (serverInfoWrapper.isPresent()) {
                    if (serverInfoWrapper.get().isOnline()) {
                        queueListenerBungee.setAuthOnline(true);
                    } else {
                        warning("Auth Server is down!!!");
                        queueListenerBungee.setAuthOnline(false);
                    }
                } else {
                    warning("Auth Server \"" + Config.AUTH_SERVER + "\" not set up!!! Check out: https://github.com/AlexProgrammerDE/PistonQueue/wiki/FAQ#server-not-set-up");
                }
            } else {
                queueListenerBungee.setAuthOnline(true);
            }
        }, 500, Config.SERVER_ONLINE_CHECK_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<PlayerWrapper> getPlayer(UUID uuid) {
        return Optional.ofNullable(getProxy().getPlayer(uuid)).map(this::wrapPlayer);
    }

    @Override
    public List<PlayerWrapper> getPlayers() {
        return getProxy().getPlayers().stream().map(this::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public Optional<ServerInfoWrapper> getServer(String name) {
        return Optional.ofNullable(getProxy().getServerInfo(name)).map(this::wrapServer);
    }

    @Override
    public void schedule(Runnable runnable, long delay, long period, TimeUnit unit) {
        getProxy().getScheduler().schedule(this, runnable, delay, period, unit);
    }

    @Override
    public void info(String message) {
        getLogger().info(message);
    }

    @Override
    public void warning(String message) {
        getLogger().warning(message);
    }

    @Override
    public void error(String message) {
        getLogger().severe(message);
    }

    private ServerInfoWrapper wrapServer(ServerInfo serverInfo) {
        PistonQueueBungee reference = this;
        return new ServerInfoWrapper() {
            @Override
            public List<PlayerWrapper> getConnectedPlayers() {
                return serverInfo.getPlayers().stream().map(reference::wrapPlayer).collect(Collectors.toList());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean isOnline() {
                try {
                    Socket s = new Socket(
                            serverInfo.getAddress().getAddress(),
                            serverInfo.getAddress().getPort());

                    s.close();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public void sendPluginMessage(String channel, byte[] data) {
                serverInfo.sendData("piston:queue", data);
            }
        };
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
                    error("Server" + server + " not found!!!");
                    return;
                }

                player.connect(optional.get());
            }

            @Override
            public Optional<String> getCurrentServer() {
                return Optional.ofNullable(player.getServer()).map(server -> server.getInfo().getName());
            }

            @Override
            public void sendMessage(MessageType type, String message) {
                ChatUtils.sendMessage(type, player, message);
            }

            @Override
            public void sendMessage(String message) {
                sendMessage(MessageType.CHAT, message);
            }

            @Override
            public void sendPlayerListHeaderAndFooter(List<String> header, List<String> footer) {
                if (header == null || footer == null) {
                    player.resetTabHeader();
                } else {
                    player.setTabHeader(ChatUtils.parseTab(header), ChatUtils.parseTab(footer));
                }
            }

            @Override
            public UUID getUniqueId() {
                return player.getUniqueId();
            }

            @Override
            public void disconnect(String message) {
                player.disconnect(ChatUtils.parseToComponent(message));
            }
        };
    }
}
