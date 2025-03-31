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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.pistonmaster.pistonqueue.bungee.commands.MainCommand;
import net.pistonmaster.pistonqueue.bungee.listeners.QueueListenerBungee;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.shared.chat.MessageType;
import net.pistonmaster.pistonqueue.shared.hooks.PistonMOTDPlaceholder;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;
import net.pistonmaster.pistonutils.update.GitHubUpdateChecker;
import net.pistonmaster.pistonutils.update.SemanticVersion;
import org.bstats.bungeecord.Metrics;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Getter
public final class PistonQueueBungee extends Plugin implements PistonQueuePlugin {
    private final QueueListenerBungee queueListenerBungee = new QueueListenerBungee(this);

    @Override
    public void onEnable() {
        PluginManager manager = getProxy().getPluginManager();

        info(ChatColor.BLUE + "Loading config");
        processConfig(getDataDirectory());

        StorageTool.setupTool(getDataDirectory());
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

        info(ChatColor.BLUE + "Loading Metrics");
        new Metrics(this, 8755);

        info(ChatColor.BLUE + "Checking for update");
        try {
            String currentVersionString = this.getDescription().getVersion();
            SemanticVersion gitHubVersion = new GitHubUpdateChecker()
                .getVersion("https://api.github.com/repos/AlexProgrammerDE/PistonQueue/releases/latest");
            SemanticVersion currentVersion = SemanticVersion.fromString(currentVersionString);

          if (gitHubVersion.isNewerThan(currentVersion)) {
              info(ChatColor.RED + "There is an update available!");
              info(ChatColor.RED + "Current version: " + currentVersionString + " New version: " + gitHubVersion);
              info(ChatColor.RED + "Download it at: https://github.com/AlexProgrammerDE/PistonQueue/releases");
          } else {
              info(ChatColor.BLUE + "You're up to date!");
          }
        } catch (IOException e) {
            error("Could not check for updates!");
            e.printStackTrace();
        }

        info(ChatColor.BLUE + "Scheduling tasks");
        scheduleTasks(queueListenerBungee);
    }

    @Override
    public Optional<PlayerWrapper> getPlayer(UUID uuid) {
        return Optional.ofNullable(getProxy().getPlayer(uuid)).map(this::wrapPlayer);
    }

    @Override
    public Optional<PlayerWrapper> getPlayer(String name) {
        return Optional.ofNullable(getProxy().getPlayer(name)).map(this::wrapPlayer);
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

    @Override
    public List<String> getAuthors() {
        return Collections.singletonList(getDescription().getAuthor());
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Path getDataDirectory() {
        return getDataFolder().toPath();
    }

    private ServerInfoWrapper wrapServer(ServerInfo serverInfo) {
        return new ServerInfoWrapper() {
            @Override
            public List<PlayerWrapper> getConnectedPlayers() {
                return serverInfo.getPlayers().stream().map(PistonQueueBungee.this::wrapPlayer).collect(Collectors.toList());
            }

            @Override
            public boolean isOnline() {
                CompletableFuture<Boolean> future = new CompletableFuture<>();
                serverInfo.ping((result, error) -> future.complete(error == null && result != null));
                return future.join();
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

                if (optional.isEmpty()) {
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
                if (message.equalsIgnoreCase("/") || message.isBlank()) {
                    return;
                }

                switch (type) {
                    case CHAT -> player.sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(message));
                    case ACTION_BAR -> player.sendMessage(ChatMessageType.ACTION_BAR, ChatUtils.parseToComponent(message));
                }
            }

            @Override
            public void sendPlayerList(List<String> header, List<String> footer) {
                player.setTabHeader(ChatUtils.parseTab(header), ChatUtils.parseTab(footer));
            }

            @Override
            public void resetPlayerList() {
                player.resetTabHeader();
            }

            @Override
            public String getName() {
                return player.getName();
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
