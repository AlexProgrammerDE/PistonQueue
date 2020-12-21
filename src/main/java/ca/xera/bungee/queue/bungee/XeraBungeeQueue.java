package ca.xera.bungee.queue.bungee;

import ca.xera.bungee.queue.bungee.commands.MainCommand;
import ca.xera.bungee.queue.bungee.listeners.QueueListener;
import ca.xera.bungee.queue.bungee.listeners.XeraListener;
import ca.xera.bungee.queue.bungee.utils.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
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
    public static final LinkedHashMap<UUID, String> regularQueue = new LinkedHashMap<>();
    public static final LinkedHashMap<UUID, String> priorityQueue = new LinkedHashMap<>();
    public static final LinkedHashMap<UUID, String> veteranQueue = new LinkedHashMap<>();

    public static BanType banType;

    @Override
    public void onEnable() {
        Logger logger = getLogger();
        PluginManager manager = getProxy().getPluginManager();
        QueueListener events = new QueueListener(this);

        logger.info(ChatColor.BLUE + "Loading config");
        processConfig();

        new StorageTool().setupTool(this);

        logger.info(ChatColor.BLUE + "Registering commands");
        manager.registerCommand(this, new MainCommand(this));

        logger.info(ChatColor.BLUE + "Registering listeners");
        manager.registerListener(this, events);
        manager.registerListener(this, new XeraListener(this));

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
            sendMessage(veteranQueue, Config.POSITIONMESSAGECHAT, ChatMessageType.CHAT);
            sendMessage(priorityQueue, Config.POSITIONMESSAGECHAT, ChatMessageType.CHAT);
            sendMessage(regularQueue, Config.POSITIONMESSAGECHAT, ChatMessageType.CHAT);
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Sends the position message and updates tab on an interval on hotbar
        getProxy().getScheduler().schedule(this, () -> {
            sendMessage(veteranQueue, Config.POSITIONMESSAGEHOTBAR, ChatMessageType.ACTION_BAR);
            sendMessage(priorityQueue, Config.POSITIONMESSAGEHOTBAR, ChatMessageType.ACTION_BAR);
            sendMessage(regularQueue, Config.POSITIONMESSAGEHOTBAR, ChatMessageType.ACTION_BAR);
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Updates the tab
        getProxy().getScheduler().schedule(this, () -> {
            updateTab(veteranQueue, Config.HEADERVETERAN, Config.FOOTERVETERAN);
            updateTab(priorityQueue, Config.HEADERPRIORITY, Config.FOOTERPRIORITY);
            updateTab(regularQueue, Config.HEADER, Config.FOOTER);
        }, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Moves the queue when someone logs off the main server on an interval set in the config.yml
        getProxy().getScheduler().schedule(this, events::moveQueue, Config.QUEUEMOVEDELAY, Config.QUEUEMOVEDELAY, TimeUnit.MILLISECONDS);

        // Checks the status of all the servers
        getProxy().getScheduler().schedule(this, () -> {
            if (getProxy().getServers().containsKey(Config.MAINSERVER)) {
                try {
                    Socket s = new Socket(
                            getProxy().getServerInfo(Config.MAINSERVER).getAddress().getAddress(),
                            getProxy().getServerInfo(Config.MAINSERVER).getAddress().getPort());

                    s.close();
                    events.mainOnline = true;
                } catch (IOException e) {
                    getLogger().warning("Main Server is down!!!");
                    events.mainOnline = false;
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
                    events.queueOnline = true;
                } catch (IOException e) {
                    getLogger().warning("Queue Server is down!!!");
                    events.queueOnline = false;
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
                        events.authOnline = true;
                    } catch (IOException e) {
                        getLogger().warning("Auth Server is down!!!");
                        events.authOnline = false;
                    }
                } else {
                    getLogger().warning("Auth Server \"" + Config.AUTHSERVER + "\" not set up!!!");
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

        banType = BanType.valueOf(config.getString("SHADOWBANTYPE"));
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
                            ChatUtils.parseToComponent(Config.QUEUEPOSITION
                                    .replaceAll("%position%", i + "")
                                    .replaceAll("%total%", queue.size() + "")
                                    .replaceAll("%server%", entry.getValue())));
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

                for (int i = 0; i < header.size(); i++) {
                    headerBuilder.append(ChatUtils.parseToString(replacePosition(header.get(i), waitTimeHour, waitTimeMinute, w)));

                    if (i != (header.size() - 1)) {
                        headerBuilder.append("\n");
                    }
                }

                for (int i = 0; i < footer.size(); i++) {
                    footerBuilder.append(ChatUtils.parseToString(replacePosition(footer.get(i), waitTimeHour, waitTimeMinute, w)));

                    if (i != (footer.size() - 1)) {
                        footerBuilder.append("\n");
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

    private String replacePosition(String text, long waitTimeHour, long waitTimeMinute, int w) {
        String format = String.format("%dh %dm", waitTimeHour, waitTimeMinute);

        if (waitTimeHour == 0)
            format = String.format("%dm", waitTimeMinute);

        return text.replaceAll("%position%", w + "").replaceAll("%wait%", format);
    }
}
