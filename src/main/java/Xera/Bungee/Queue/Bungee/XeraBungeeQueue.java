package Xera.Bungee.Queue.Bungee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

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

        logger.info("§9Loading config");
        processConfig();

        logger.info("§9Registering commands");
        manager.registerCommand(this, new ReloadCommand(this));

        logger.info("§9Registering listeners");
        manager.registerListener(this, new BungeeEvents());
        manager.registerListener(this, new PingEvent(this));

        logger.info("§9Loading Metrics");
        new Metrics(this, 8519);

        logger.info("§9Checking for update");
        new UpdateChecker(this, 74615).getVersion(version -> {
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                logger.info("§9Your up to date!");
            } else {
                logger.info("§cThere is a update available.");
                logger.info("§cCurrent version: " + this.getDescription().getVersion() + " New version: " + version);
                logger.info("§cDownload it at: https://www.spigotmc.org/resources/74615");
            }
        });

        logger.info("§9Scheduling tasks");
        //sends the position message and updates tab on an interval for non priority players and priority players in chat
        getProxy().getScheduler().schedule(this, () -> {
            if (!Lang.POSITIONMESSAGEHOTBAR) {

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
                                TextComponent.fromLegacyText(Lang.QUEUEPOSITION.replace("&", "§")
                                        .replace("<position>", i + "").replace("<total>",
                                                regularqueue.size() + "").replace("<server>",
                                                entry.getValue())));
                    } catch (Exception e) {
                        regularqueue.remove(entry.getKey());
                        //TODO: handle exception
                    }
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (!Lang.POSITIONMESSAGEHOTBAR) {

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
                                TextComponent.fromLegacyText(Lang.QUEUEPOSITION.replace("&", "§")
                                        .replace("<position>", i + "").replace("<total>",
                                                regularqueue.size() + "").replace("<server>",
                                                entry2.getValue())));

                    } catch (Exception e) {
                        priorityqueue.remove(entry2.getKey());
                        //TODO: handle exception
                    }
                }
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        //sends the position message and updates tab on an interval for non priority players and priority players on hotbar
        getProxy().getScheduler().schedule(this, () -> {
            if (Lang.POSITIONMESSAGEHOTBAR) {

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
                                TextComponent.fromLegacyText(Lang.QUEUEPOSITION.replace("&", "§")
                                        .replace("<position>",
                                                i + "").replace("<total>",
                                                regularqueue.size() + "").replace("<server>",
                                                entry.getValue())));
                    } catch(Exception e){
                        regularqueue.remove(entry.getKey());
                        //TODO: handle exception
                    }
                }
            }
        }, Lang.QUEUEMOVEDELAY, Lang.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (Lang.POSITIONMESSAGEHOTBAR) {

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
                                TextComponent.fromLegacyText(Lang.QUEUEPOSITION.replace("&", "§")
                                        .replace("<position>",
                                                i + "").replace("<total>",
                                                regularqueue.size() + "").replace("<server>",
                                                entry2.getValue())));
                    } catch (Exception e) {
                        priorityqueue.remove(entry2.getKey());
                        //TODO: handle exception
                    }
                }
            }
        }, Lang.QUEUEMOVEDELAY, Lang.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        //updates the tablists for priority and regular queues
        getProxy().getScheduler().schedule(this, () -> {

            int i = 0;
            long waitTime;
            long waitTimeHour;
            long waitTimeMinute;

            Map<UUID, String> the_map = new LinkedHashMap<>(regularqueue);
            for (Entry<UUID, String> entry : the_map.entrySet()) {
                try {
                    i++;

                    ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                    if (player == null) {
                        regularqueue.remove(entry.getKey());
                        continue;
                    }

                    waitTime = i;

                    waitTimeHour = waitTime / 60;
                    waitTimeMinute = waitTime % 60;
                    if (waitTimeHour == 0) {
                        player.setTabHeader(
                                new ComponentBuilder(Lang.HEADER.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dm", waitTimeMinute) + "")).create(),
                                new ComponentBuilder(Lang.FOOTER.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dm", waitTimeMinute)) + "").create());
                    } else {
                        player.setTabHeader(
                                new ComponentBuilder(Lang.HEADER.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute) + "")).create(),
                                new ComponentBuilder(Lang.FOOTER.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)) + "").create());
                    }

                } catch (Exception e) {
                    regularqueue.remove(entry.getKey());
                    //TODO: handle exception
                }
            }
        }, Lang.QUEUEMOVEDELAY, Lang.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            int i = 0;
            long waitTime;
            long waitTimeHour;
            long waitTimeMinute;

            Map<UUID, String> the_map = new LinkedHashMap<>(priorityqueue);
            for (Entry<UUID, String> entry2 : the_map.entrySet()) {
                try {
                    i++;

                    ProxiedPlayer player = getProxy().getPlayer(entry2.getKey());
                    if (player == null) {
                        priorityqueue.remove(entry2.getKey());
                        continue;
                    }

                    waitTime = i;

                    waitTimeHour = waitTime / 60;
                    waitTimeMinute = waitTime % 60;
                    if (waitTimeHour == 0) {
                        player.setTabHeader(
                                new ComponentBuilder(Lang.HEADERPRIORITY.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dm", waitTimeMinute) + "")).create(),
                                new ComponentBuilder(Lang.FOOTERPRIORITY.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dm", waitTimeMinute)) + "").create());
                    } else {
                        player.setTabHeader(
                                new ComponentBuilder(Lang.HEADERPRIORITY.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute) + "")).create(),
                                new ComponentBuilder(Lang.FOOTERPRIORITY.replace("&", "§")
                                        .replace("<position>", i + "")
                                        .replace("<wait>", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute)) + "").create());
                    }
                } catch (Exception e) {
                    priorityqueue.remove(entry2.getKey());
                    //TODO: handle exception
                }
            }
        }, Lang.QUEUEMOVEDELAY, Lang.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        //moves the queue when someone logs off the main server on an interval set in the bungeeconfig.yml
        try {
            getProxy().getScheduler().schedule(this, BungeeEvents::moveQueue, Lang.QUEUEMOVEDELAY, Lang.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);
        } catch (NoSuchElementException ignored) {
        }

        //moves the queue when someone logs off the main server on an interval set in the bungeeconfig.yml
        try {
            getProxy().getScheduler().schedule(this, BungeeEvents::CheckIfMainServerIsOnline,500, 500, TimeUnit.MILLISECONDS);
        } catch (NoSuchElementException ignored) {
        }

        try {
            getProxy().getScheduler().schedule(this, BungeeEvents::CheckIfQueueServerIsOnline, 500, 500, TimeUnit.MILLISECONDS);
        } catch (NoSuchElementException ignored) {
        }

        try {
            getProxy().getScheduler().schedule(this, BungeeEvents::CheckIfAuthServerIsOnline, 500, 500, TimeUnit.MILLISECONDS);
        } catch (NoSuchElementException ignored) {
        }
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
        Arrays.asList(Lang.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);
                it.set(Lang.class, config.get(it.getName()));
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }
}
