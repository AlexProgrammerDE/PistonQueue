package ca.xera.bungee.queue.bungee.utils;

import ca.xera.bungee.queue.bungee.XeraBungeeQueue;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StorageTool {
    private static XeraBungeeQueue plugin;
    private static Configuration dataConfig;
    private static File dataFile;

    /**
     * Shadowban a player!
     * @param player The player to shadowban.
     * @param date The date when he will be unbanned.
     * @return true if player got shadow banned and if already shadow banned false.
     */
    public static boolean shadowBanPlayer(ProxiedPlayer player, Date date) {
        manageBan(player);

        if (!dataConfig.contains(player.getUniqueId().toString())) {
            dataConfig.set(player.getUniqueId().toString(), date.toString());

            saveData();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Unshadowban a player!
     * @param player The player to unshadowban.
     * @return true if player got unshadowbanned and false if not was shadow banned.
     */
    public static boolean unShadowBanPlayer(ProxiedPlayer player) {
        if (dataConfig.contains(player.getUniqueId().toString())) {
            dataConfig.set(player.getUniqueId().toString(), null);

            saveData();

            return true;
        } else {
            return false;
        }
    }

    public static boolean isShadowBanned(ProxiedPlayer player) {
        manageBan(player);

        return dataConfig.contains(player.getUniqueId().toString());
    }

    private static void manageBan(ProxiedPlayer player) {
        Date now = new Date();

        if (dataConfig.contains(player.getUniqueId().toString())) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", new Locale("us"));

            try {
                Date date = sdf.parse(dataConfig.getString(player.getUniqueId().toString()));

                if (now.after(date) || (now.equals(date))) {
                    unShadowBanPlayer(player);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private static void loadData() {
        generateFile();

        try {
            dataConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(dataFile);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void saveData() {
        generateFile();

        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(dataConfig, dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateFile() {
        if (!plugin.getDataFolder().exists()) {
            if (!plugin.getDataFolder().mkdir())
                return;
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupTool(XeraBungeeQueue plugin) {
        StorageTool.plugin = plugin;
        StorageTool.dataFile = new File(plugin.getDataFolder(), "data.yml");

        loadData();
    }
}
