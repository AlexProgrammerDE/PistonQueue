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
package net.pistonmaster.pistonqueue.bungee.utils;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.pistonmaster.pistonqueue.bungee.PistonQueue;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class StorageTool {
    private static PistonQueue plugin;
    private static Configuration dataConfig;
    private static File dataFile;

    private StorageTool() {
    }

    /**
     * Shadowban a player!
     *
     * @param player The player to shadowban.
     * @param date   The date when he will be unbanned.
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
     *
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
        } catch (IOException e) {
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
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdir())
            return;

        if (!dataFile.exists()) {
            try {
                if (!dataFile.createNewFile())
                    throw new IOException("Couldn't create file " + dataFile.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void setupTool(PistonQueue plugin) {
        StorageTool.plugin = plugin;
        StorageTool.dataFile = new File(plugin.getDataFolder(), "data.yml");

        loadData();
    }
}
