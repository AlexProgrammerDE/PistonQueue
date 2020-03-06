package Leees.Bungee.Queue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.xml.internal.org.jvnet.mimepull.Header;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.tab.TabList;
import Leees.Bungee.Queue.events.Lang;
import Leees.Bungee.Queue.events.Events;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.tab.*;

/**
 * QueuePlugin
 */
public class QueuePlugin extends Plugin {
    int delay;
    public static LinkedHashMap<UUID, String> final_destination = new LinkedHashMap<>();
    public Configuration config;

    private void updateBungeeConfig(int slots) throws ReflectiveOperationException {
        Method setMethod = getProxy().getConfigurationAdapter().getClass().getDeclaredMethod("set", String.class,
            Object.class);
        setMethod.setAccessible(true);
        setMethod.invoke(getProxy().getConfigurationAdapter(), "player_limit", slots);
    }

    private void changeSlots(int slots) throws ReflectiveOperationException {
        Class<?> configClass = getProxy().getConfig().getClass();

        if (!configClass.getSuperclass().equals(Object.class)) {
            configClass = configClass.getSuperclass();
        }

        Field playerLimitField = configClass.getDeclaredField("playerLimit");
        playerLimitField.setAccessible(true);
        playerLimitField.set(getProxy().getConfig(), slots);
    }
    @Override
    public void onEnable() {
        processConfig();
        Arrays.asList(Lang.class.getDeclaredFields()).forEach(it -> {
            try {
                it.setAccessible(true);
                it.set(Lang.class, config.get(it.getName()));
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        try {
            updateBungeeConfig(Lang.SUPER_SLOTS);
            changeSlots(Lang.SUPER_SLOTS);
        } catch (ReflectiveOperationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (Lang.POSITIONMESSAGEHOTBAR.equals("true")) {
            delay = 700;
        } else {
            delay = 10000;
        }
        getProxy().getPluginManager().registerListener(this, new Events());
        getProxy().getScheduler().schedule(this, () -> {

            int i = 0;
            Map<UUID, String> the_map = new LinkedHashMap<>(final_destination);
            for (Entry<UUID, String> entry : the_map.entrySet()) {
                try {
                    i++;

                    ProxiedPlayer player = getProxy().getPlayer(entry.getKey());
                    if (player == null) {
                        final_destination.remove(entry.getKey());
                        continue;
                    }
                    if (Lang.POSITIONMESSAGEHOTBAR.equals("true")) {
                        player.sendMessage(ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText("§6" +
                                        Lang.CURRENT_LIMBO_POSITION.replace("<position>",
                                                "§l" + i + "").replace("<total>",
                                                final_destination.size() + "").replace("<server>",
                                                entry.getValue())));
                    } else {
                        player.sendMessage(ChatMessageType.CHAT,
                                TextComponent.fromLegacyText("§6" +
                                        Lang.CURRENT_LIMBO_POSITION.replace("<position>",
                                                "§l" + i + "").replace("<total>",
                                                final_destination.size() + "").replace("<server>",
                                                entry.getValue())));
                    }
                    player.setTabHeader(
                            new ComponentBuilder("\n" + Lang.TABNAME + "\n\n§6" + Lang.MESSAGE1 + "\n§6" +
                                    Lang.CURRENT_LIMBO_POSITION.replace("<position>",
                                            "§l" + i + "\n").replace("<total>",
                                            final_destination.size() + "").replace("<server>",
                                            entry.getValue())).create(),
                    new ComponentBuilder("\n§6" + Lang.MESSAGE2 + "\n\n§7Contact: " +
                            Lang.CONTACT + "\n§7Discussion: " + Lang.DISCUSSION + "\n§7Website: " +
                            Lang.WEBSITE + "\n§7" + Lang.OFFICIAL + "\n").create()
                    );

                } catch (Exception e) {
                    final_destination.remove(entry.getKey());
                    //TODO: handle exception
                }
            }
        },delay,delay, TimeUnit.MILLISECONDS);
        getProxy().getScheduler().schedule(this, () -> {
            Events.moveQueue();
        }, 700, 700, TimeUnit.MILLISECONDS);
    }


    void processConfig() {
        try {
            loadConfig();
        } catch (IOException e) {
            if (!getDataFolder().exists())
                getDataFolder().mkdir();
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, file.toPath());
                    loadConfig();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config,
                new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    void loadConfig() throws IOException {
        config = ConfigurationProvider.getProvider(YamlConfiguration.class)
            .load(new File(getDataFolder(), "config.yml"));

    }
}
