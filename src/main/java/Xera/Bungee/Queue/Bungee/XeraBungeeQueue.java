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
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * XeraBungeeQueue
 */
@SuppressWarnings({"deprecation"})
public final class XeraBungeeQueue extends Plugin {
    protected static final LinkedHashMap<UUID, String> regularQueue = new LinkedHashMap<>();
    protected static final LinkedHashMap<UUID, String> priorityQueue = new LinkedHashMap<>();
    protected static final LinkedHashMap<UUID, String> veteranQueue = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        PluginManager manager = getProxy().getPluginManager();
        ProxyListener events = new ProxyListener();

        logger.info("§9Loading config");
        processConfig();

        logger.info("§9Registering commands");
        manager.registerCommand(this, new MainCommand(this));

        logger.info("§9Registering listeners");
        manager.registerListener(this, events);
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

        // Sends the position message and updates tab on an interval in chat
        getProxy().getScheduler().schedule(this, () -> {
            sendMessage(regularQueue, Config.POSITIONMESSAGECHAT, ChatMessageType.CHAT);
            sendMessage(priorityQueue, Config.POSITIONMESSAGECHAT, ChatMessageType.CHAT);
            sendMessage(veteranQueue, Config.POSITIONMESSAGECHAT, ChatMessageType.CHAT);
        }, 10000, 10000, TimeUnit.MILLISECONDS);

        // Sends the position message and updates tab on an interval on hotbar
        getProxy().getScheduler().schedule(this, () -> {
            sendMessage(regularQueue, Config.POSITIONMESSAGEHOTBAR, ChatMessageType.ACTION_BAR);
            sendMessage(priorityQueue, Config.POSITIONMESSAGEHOTBAR, ChatMessageType.ACTION_BAR);
            sendMessage(veteranQueue, Config.POSITIONMESSAGEHOTBAR, ChatMessageType.ACTION_BAR);
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Updates the tab
        // Regular
        getProxy().getScheduler().schedule(this, () -> {
            updateTab(regularQueue, Config.HEADER, Config.FOOTER);
            updateTab(priorityQueue, Config.HEADERPRIORITY, Config.FOOTERPRIORITY);
            updateTab(veteranQueue, Config.HEADERVETERAN, Config.FOOTERVETERAN);
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // moves the queue when someone logs off the main server on an interval set in the bungeeconfig.yml
        getProxy().getScheduler().schedule(this, events::moveQueue, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);


        // moves the queue when someone logs off the main server on an interval set in the bungeeconfig.yml
        getProxy().getScheduler().schedule(this, () -> {
            try {
                Socket s = new Socket(
                        ProxyServer.getInstance().getServerInfo(Config.MAINSERVER).getAddress().getAddress(),
                        ProxyServer.getInstance().getServerInfo(Config.MAINSERVER).getAddress().getPort());

                s.close();
                events.mainOnline = true;
            } catch (IOException e) {
                getLogger().warning("Main Server is down!!!");
                events.mainOnline = false;
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            try {
                Socket s = new Socket(
                        ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER).getAddress().getAddress(),
                        ProxyServer.getInstance().getServerInfo(Config.QUEUESERVER).getAddress().getPort());

                s.close();
                events.queueOnline = true;
            } catch (IOException e) {
                getLogger().warning("Queue Server is down!!!");
                events.queueOnline = false;
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);

        getProxy().getScheduler().schedule(this, () -> {
            if (Config.ENABLEAUTHSERVER) {
                try {
                    Socket s = new Socket(
                            ProxyServer.getInstance().getServerInfo(Config.AUTHSERVER).getAddress().getAddress(),
                            ProxyServer.getInstance().getServerInfo(Config.AUTHSERVER).getAddress().getPort());

                    s.close();
                    events.authOnline = true;
                } catch (IOException e) {
                    getLogger().warning("Auth Server is down!!!");
                    events.authOnline = false;
                }
            } else {
                events.authOnline = true;
            }
        }, 500, Config.SERVERONLINECHECKDELAY, TimeUnit.MILLISECONDS);
    }

    public void processConfig() {
        try {
            loadConfig();
        } catch (IOException e) {
            if (!getDataFolder().exists() && !getDataFolder().mkdir()) {
                return;
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

    private void loadConfig() throws IOException {
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        Arrays.asList(Config.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);
                it.set(Config.class, config.get(it.getName()));
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    protected static String parseText(String text) {
        String returnedText = text;

        returnedText = returnedText.replaceAll("%servername%", Config.SERVERNAME);
        returnedText = returnedText.replaceAll("%regular%", String.valueOf(QueueAPI.getRegularSize()));
        returnedText = returnedText.replaceAll("%priority%", String.valueOf(QueueAPI.getPrioritySize()));
        returnedText = returnedText.replaceAll("%veteran%", String.valueOf(QueueAPI.getVeteranSize()));

        return returnedText;
    }

    private void sendMessage(LinkedHashMap<UUID, String> queue, boolean bool, ChatMessageType type) {
        if (!bool) {
            int i = 0;

            Map<UUID, String> the_map = new LinkedHashMap<>(queue);
            for (Entry<UUID, String> entry : the_map.entrySet()) {
                try {
                    i++;

                    ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                    if (player == null) {
                        queue.remove(entry.getKey());
                        continue;
                    }

                    player.sendMessage(type,
                            TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(Config.QUEUEPOSITION)
                                    .replaceAll("%position%", i + "")
                                    .replaceAll("%total%", queue.size() + "")
                                    .replaceAll("%server%", entry.getValue()))));
                } catch (Exception e) {
                    queue.remove(entry.getKey());
                }
            }
        }
    }

    private void updateTab(LinkedHashMap<UUID, String> queue, List<String> header, List<String> footer) {
        int w = 0;
        long waitTime;
        long waitTimeHour;
        long waitTimeMinute;

        Map<UUID, String> the_map = new LinkedHashMap<>(queue);
        for (Entry<UUID, String> entry : the_map.entrySet()) {
            try {
                w++;

                ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                if (player == null) {
                    queue.remove(entry.getKey());
                    continue;
                }

                waitTime = w;

                waitTimeHour = waitTime / 60;
                waitTimeMinute = waitTime % 60;

                StringBuilder headerBuilder = new StringBuilder();
                StringBuilder footerBuilder = new StringBuilder();

                if (waitTimeHour == 0) {
                    for (int i = 0; i < header.size(); i++) {
                        if (i == (header.size() - 1)) {
                            headerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(header.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute))));
                        } else {
                            headerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(header.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute))))
                                    .append("\n");
                        }
                    }

                    for (int i = 0; i < footer.size(); i++) {
                        if (i == (footer.size() - 1)) {
                            footerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(footer.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute))));
                        } else {
                            footerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(footer.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute))))
                                    .append("\n");
                        }
                    }

                } else {
                    for (int i = 0; i < header.size(); i++) {
                        if (i == (header.size() - 1)) {
                            headerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(header.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute))));
                        } else {
                            headerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(header.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute))))
                                    .append("\n");
                        }
                    }

                    for (int i = 0; i < footer.size(); i++) {
                        if (i == (footer.size() - 1)) {
                            footerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(footer.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dh %dm", waitTimeHour, waitTimeMinute))));
                        } else {
                            footerBuilder.append(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(footer.get(i))
                                    .replaceAll("%position%", w + "")
                                    .replaceAll("%wait%", "" + String.format("%dm", waitTimeMinute))))
                                    .append("\n");
                        }
                    }
                }

                player.setTabHeader(
                        new ComponentBuilder(headerBuilder.toString()).create(),
                        new ComponentBuilder(footerBuilder.toString()).create());

            } catch (Exception e) {
                queue.remove(entry.getKey());
            }
        }
    }
}
