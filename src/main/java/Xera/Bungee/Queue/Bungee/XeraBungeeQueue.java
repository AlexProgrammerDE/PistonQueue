package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * XeraBungeeQueue
 */
public class XeraBungeeQueue extends Plugin {
    public static LinkedHashMap<UUID, String> regularqueue = new LinkedHashMap<>();
    public static LinkedHashMap<UUID, String> priorityqueue = new LinkedHashMap<>();
    public Configuration config;

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        PluginManager manager = getProxy().getPluginManager();
        BungeeEvents.plugin = this;

        logger.info("§9Loading config");
        processConfig();

        logger.info("§9Registering commands");
        manager.registerCommand(this, new MainCommand(this));

        logger.info("§9Registering listeners");
        manager.registerListener(this, new BungeeEvents());
        manager.registerListener(this, new PingEvent(this));

        logger.info("§9Loading Metrics");
        new Metrics(this, 8755);

        logger.info("§9Checking for update");
        new UpdateChecker(this, 83541).getVersion(version -> {
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                logger.info("§9Your up to date!");
            } else {
                logger.info("§cThere is a update available.");
                logger.info("§cCurrent version: " + this.getDescription().getVersion() + " New version: " + version);
                logger.info("§cDownload it at: https://www.spigotmc.org/resources/83541");
            }
        });

        logger.info("§9Scheduling tasks");

        // sends the position message and updates tab on an interval for non priority players and priority players in chat
        getProxy().getScheduler().schedule(this, () -> {
            if (!Config.POSITIONMESSAGEHOTBAR) {
                int i = 0;

                Map<UUID, String> the_map = new LinkedHashMap<>(regularqueue);
                for (Entry<UUID, String> entry : the_map.entrySet()) {
                    try {
                        i++;

                        ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                        if (player == null) {
                            regularqueue.remove(entry.getKey());
                            continue;
                        }

                        player.sendMessage(ChatMessageType.CHAT,
                                TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', Config.QUEUEPOSITION)
                                        .replaceAll("%position%", i + "")
                                        .replaceAll("%total%", regularqueue.size() + "")
                                        .replaceAll("%server%", entry.getValue())));
                    } catch (Exception e) {
                        regularqueue.remove(entry.getKey());
                        // TODO: handle exception
                    }
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (!Config.POSITIONMESSAGEHOTBAR) {

                int i = 0;

                Map<UUID, String> the_map = new LinkedHashMap<>(priorityqueue);
                for (Entry<UUID, String> entry2 : the_map.entrySet()) {
                    try {
                        i++;

                        ProxiedPlayer player = getProxy().getPlayer(entry2.getKey());
                        if (player == null) {
                            priorityqueue.remove(entry2.getKey());
                            continue;
                        }

                        player.sendMessage(ChatMessageType.CHAT,
                                TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', Config.QUEUEPOSITION)
                                        .replaceAll("%position%", i + "")
                                        .replaceAll("%total%", regularqueue.size() + "")
                                        .replaceAll("%server%", entry2.getValue())));

                    } catch (Exception e) {
                        priorityqueue.remove(entry2.getKey());
                        // TODO: handle exception
                    }
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        // sends the position message and updates tab on an interval for non priority players and priority players on hotbar
        getProxy().getScheduler().schedule(this, () -> {
            if (Config.POSITIONMESSAGEHOTBAR) {

                int i = 0;

                Map<UUID, String> the_map = new LinkedHashMap<>(regularqueue);
                for (Entry<UUID, String> entry : the_map.entrySet()) {
                    try {
                        i++;

                        ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                        if (player == null) {
                            regularqueue.remove(entry.getKey());
                            continue;
                        }

                        player.sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', Config.QUEUEPOSITION)
                                        .replaceAll("%position%", i + "")
                                        .replaceAll("%total%", regularqueue.size() + "")
                                        .replaceAll("%server%", entry.getValue())));
                    } catch(Exception e){
                        regularqueue.remove(entry.getKey());
                        // TODO: handle exception
                    }
                }
            }
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (Config.POSITIONMESSAGEHOTBAR) {

                int i = 0;

                Map<UUID, String> the_map = new LinkedHashMap<>(priorityqueue);
                for (Entry<UUID, String> entry2 : the_map.entrySet()) {
                    try {
                        i++;

                        ProxiedPlayer player = getProxy().getPlayer(entry2.getKey());
                        if (player == null) {
                            priorityqueue.remove(entry2.getKey());
                            continue;
                        }
                        player.sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', Config.QUEUEPOSITION)
                                        .replaceAll("%position%", i + "")
                                        .replaceAll("%total%", regularqueue.size() + "")
                                        .replaceAll("%server%", entry2.getValue())));
                    } catch (Exception e) {
                        priorityqueue.remove(entry2.getKey());
                        // TODO: handle exception
                    }
                }
            }
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // updates the playerlist for priority and regular queues
        getProxy().getScheduler().schedule(this, () -> {

            int w = 0;
            long waitTime;
            long waitTimeHour;
            long waitTimeMinute;

            Map<UUID, String> the_map = new LinkedHashMap<>(regularqueue);
            for (Entry<UUID, String> entry : the_map.entrySet()) {
                try {
                    w++;

                    ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                    if (player == null) {
                        regularqueue.remove(entry.getKey());
                        continue;
                    }

                    waitTime = w;

                    waitTimeHour = waitTime / 60;
                    waitTimeMinute = waitTime % 60;

                    StringBuilder header = new StringBuilder();
                    StringBuilder footer = new StringBuilder();

                    if (waitTimeHour == 0) {
                        for (int i = 0; i < Config.HEADER.size(); i++) {
                            if (i == (Config.HEADER.size() - 1)) {
                                header.append(ChatColor.translateAlternateColorCodes('&', Config.HEADER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)));
                            } else {
                                header.append(ChatColor.translateAlternateColorCodes('&', Config.HEADER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                        for (int i = 0; i < Config.FOOTER.size(); i++) {
                            if (i == (Config.FOOTER.size() - 1)) {
                                footer.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)));
                            } else {
                                footer.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                    } else {
                        for (int i = 0; i < Config.HEADER.size(); i++) {
                            if (i == (Config.HEADER.size() - 1)) {
                                header.append(ChatColor.translateAlternateColorCodes('&', Config.HEADER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)));
                            } else {
                                header.append(ChatColor.translateAlternateColorCodes('&', Config.HEADER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                        for (int i = 0; i < Config.FOOTER.size(); i++) {
                            if (i == (Config.FOOTER.size() - 1)) {
                                footer.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)));
                            } else {
                                footer.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTER.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                    }

                    player.setTabHeader(
                            new ComponentBuilder(header.toString()).create(),
                            new ComponentBuilder(footer.toString()).create());

                } catch (Exception e) {
                    regularqueue.remove(entry.getKey());
                    // TODO: handle exception
                }
            }
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            int w = 0;
            long waitTime;
            long waitTimeHour;
            long waitTimeMinute;

            Map<UUID, String> the_map = new LinkedHashMap<>(priorityqueue);
            for (Entry<UUID, String> entry2 : the_map.entrySet()) {
                try {
                    w++;

                    ProxiedPlayer player = getProxy().getPlayer(entry2.getKey());
                    if (player == null) {
                        priorityqueue.remove(entry2.getKey());
                        continue;
                    }

                    waitTime = w;

                    waitTimeHour = waitTime / 60;
                    waitTimeMinute = waitTime % 60;

                    StringBuilder headerprio = new StringBuilder();
                    StringBuilder footerprio = new StringBuilder();

                    if (waitTimeHour == 0) {
                        for (int i = 0; i < Config.HEADERPRIORITY.size(); i++) {
                            if (i == (Config.HEADERPRIORITY.size() - 1)) {
                                headerprio.append(ChatColor.translateAlternateColorCodes('&', Config.HEADERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)));
                            } else {
                                headerprio.append(ChatColor.translateAlternateColorCodes('&', Config.HEADERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                        for (int i = 0; i < Config.FOOTERPRIORITY.size(); i++) {
                            if (i == (Config.FOOTERPRIORITY.size() - 1)) {
                                footerprio.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)));
                            } else {
                                footerprio.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                    } else {
                        for (int i = 0; i < Config.HEADER.size(); i++) {
                            if (i == (Config.HEADER.size() - 1)) {
                                headerprio.append(ChatColor.translateAlternateColorCodes('&', Config.HEADERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)));
                            } else {
                                headerprio.append(ChatColor.translateAlternateColorCodes('&', Config.HEADERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                        for (int i = 0; i < Config.FOOTERPRIORITY.size(); i++) {
                            if (i == (Config.FOOTERPRIORITY.size() - 1)) {
                                footerprio.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)));
                            } else {
                                footerprio.append(ChatColor.translateAlternateColorCodes('&', Config.FOOTERPRIORITY.get(i))
                                        .replaceAll("%position%", w + "")
                                        .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute)))
                                        .append("\n");
                            }
                        }

                    }

                    player.setTabHeader(
                            new ComponentBuilder(headerprio.toString()).create(),
                            new ComponentBuilder(footerprio.toString()).create());
                } catch (Exception e) {
                    priorityqueue.remove(entry2.getKey());
                    // TODO: handle exception
                }
            }
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // moves the queue when someone logs off the main server on an interval set in the bungeeconfig.yml
        getProxy().getScheduler().schedule(this, BungeeEvents::moveQueue, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // moves the queue when someone logs off the main server on an interval set in the bungeeconfig.yml
        getProxy().getScheduler().schedule(this, () -> ProxyServer.getInstance().getServerInfo(Config.MAINSERVER).ping((result, error) -> {
            if (error != null) {
                getLogger().warning("Main Server is down!!!");
            }

            BungeeEvents.mainonline = error == null;
        }), 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER).ping((result, error) -> {
            if (error != null) {
                getLogger().warning("Queue Server is down!!!");
            }

            BungeeEvents.queueonline = error == null;
        }), 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (Config.ENABLEAUTHSERVER) {
                ProxyServer.getInstance().getServerInfo(Config.AUTHSERVER).ping((result, error) -> {
                    if (error != null) {
                        getLogger().warning("Auth Server is down!!!");
                    }

                    BungeeEvents.authonline = error == null;
                });
            } else {
                BungeeEvents.authonline = true;
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);
    }

    void processConfig() {
        try {
            loadConfig();
        } catch (IOException e) {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

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

    void loadConfig() throws IOException {
        config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);
                it.set(Config.class, config.get(it.getName()));
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }
}
