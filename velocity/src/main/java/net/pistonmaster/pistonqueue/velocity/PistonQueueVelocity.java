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
import net.kyori.adventure.text.Component;
import net.pistonmaster.pistonqueue.data.PluginData;
import net.pistonmaster.pistonqueue.shared.chat.MessageType;
import net.pistonmaster.pistonqueue.shared.hooks.PistonMOTDPlaceholder;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;
import net.pistonmaster.pistonqueue.velocity.commands.MainCommand;
import net.pistonmaster.pistonqueue.velocity.listeners.QueueListenerVelocity;
import net.pistonmaster.pistonqueue.velocity.utils.ChatUtils;
import net.pistonmaster.pistonutils.update.GitHubUpdateChecker;
import net.pistonmaster.pistonutils.update.SemanticVersion;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Plugin(id = "pistonqueue", name = PluginData.NAME, version = PluginData.VERSION,
        url = PluginData.URL, description = PluginData.DESCRIPTION, authors = {"AlexProgrammerDE"})
public final class PistonQueueVelocity implements PistonQueuePlugin {
    @Getter
    private final Path dataDirectory;
    @Getter
    private final ProxyServer proxyServer;
    @Getter
    private final Logger logger;
    @Getter
    private final PluginContainer pluginContainer;
    @Getter
    private final QueueListenerVelocity queueListenerVelocity = new QueueListenerVelocity(this);
    private final Metrics.Factory metricsFactory;

    @Inject
    public PistonQueueVelocity(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory, PluginContainer pluginContainer, Metrics.Factory metricsFactory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginContainer = pluginContainer;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        info("Loading config");
        processConfig(dataDirectory);

        StorageTool.setupTool(dataDirectory);
        initializeReservationSlots();

        info("Looking for hooks");
        if (proxyServer.getPluginManager().getPlugin("pistonmotd").isPresent()) {
            info("Hooking into PistonMOTD");
            new PistonMOTDPlaceholder();
        }

        info("Registering plugin messaging channel");
        proxyServer.getChannelRegistrar().register(MinecraftChannelIdentifier.from("piston:queue"));

        info("Registering commands");
        proxyServer.getCommandManager().register("pistonqueue", new MainCommand(this), "pq");

        info("Registering listeners");
        proxyServer.getEventManager().register(this, queueListenerVelocity);

        info("Loading Metrics");
        metricsFactory.make(this, 12389);

        info("Checking for update");
        try {
            String currentVersionString = pluginContainer.getDescription().getVersion().orElse("unknown");
            SemanticVersion gitHubVersion = new GitHubUpdateChecker()
                .getVersion("https://api.github.com/repos/AlexProgrammerDE/PistonQueue/releases/latest");
            SemanticVersion currentVersion = SemanticVersion.fromString(currentVersionString);

            if (gitHubVersion.isNewerThan(currentVersion)) {
                info("You're up to date!");
            } else {
                info("There is an update available!");
                info("Current version: " + currentVersionString + " New version: " + gitHubVersion);
                info("Download it at: https://github.com/AlexProgrammerDE/PistonQueue/releases");
            }
        } catch (IOException e) {
            error("Could not check for updates!");
            e.printStackTrace();
        }

        info("Scheduling tasks");
        scheduleTasks(queueListenerVelocity);
    }

    @Override
    public Optional<PlayerWrapper> getPlayer(UUID uuid) {
        return proxyServer.getPlayer(uuid).map(this::wrapPlayer);
    }

    @Override
    public Optional<PlayerWrapper> getPlayer(String name) {
        return proxyServer.getPlayer(name).map(this::wrapPlayer);
    }

    @Override
    public List<PlayerWrapper> getPlayers() {
        return proxyServer.getAllPlayers().stream().map(this::wrapPlayer).collect(Collectors.toList());
    }

    @Override
    public Optional<ServerInfoWrapper> getServer(String name) {
        return proxyServer.getServer(name).map(this::wrapServer);
    }

    @Override
    public void schedule(Runnable runnable, long delay, long period, TimeUnit unit) {
        proxyServer.getScheduler().buildTask(this, runnable).delay(delay, unit).repeat(period, unit).schedule();
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public List<String> getAuthors() {
        return pluginContainer.getDescription().getAuthors();
    }

    @Override
    public String getVersion() {
        return pluginContainer.getDescription().getVersion().orElse("unknown");
    }

    private ServerInfoWrapper wrapServer(RegisteredServer server) {
        return new ServerInfoWrapper() {
            @Override
            public List<PlayerWrapper> getConnectedPlayers() {
                return server.getPlayersConnected().stream().map(PistonQueueVelocity.this::wrapPlayer).collect(Collectors.toList());
            }

            @Override
            public boolean isOnline() {
                try {
                    server.ping().join();
                    return true;
                } catch (CancellationException | CompletionException e) {
                    return false;
                }
            }

            @Override
            public void sendPluginMessage(String channel, byte[] data) {
                server.sendPluginMessage(() -> "piston:queue", data);
            }
        };
    }

    public PlayerWrapper wrapPlayer(Player player) {
        return new PlayerWrapper() {
            @Override
            public boolean hasPermission(String node) {
                return player.hasPermission(node);
            }

            @Override
            public void connect(String server) {
                Optional<RegisteredServer> optional = proxyServer.getServer(server);

                if (!optional.isPresent()) {
                    error("Server" + server + " not found!!!");
                    return;
                }

                player.createConnectionRequest(optional.get()).connect();
            }

            @Override
            public Optional<String> getCurrentServer() {
                if (player.getCurrentServer().isPresent()) {
                    return Optional.of(player.getCurrentServer().get().getServerInfo().getName());
                } else {
                    return Optional.empty();
                }
            }

            @Override
            public void sendMessage(MessageType type, String message) {
                switch (type) {
                    case CHAT:
                        ChatUtils.sendMessage(MessageType.CHAT, player, message);
                        break;
                    case ACTION_BAR:
                        ChatUtils.sendMessage(MessageType.ACTION_BAR, player, message);
                        break;
                }
            }

            @Override
            public void sendPlayerList(List<String> header, List<String> footer) {
                player.sendPlayerListHeaderAndFooter(ChatUtils.parseTab(header), ChatUtils.parseTab(footer));
            }

            @Override
            public void resetPlayerList() {
                player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            }

            @Override
            public String getName() {
                return player.getUsername();
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
