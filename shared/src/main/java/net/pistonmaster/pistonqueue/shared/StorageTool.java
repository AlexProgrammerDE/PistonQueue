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
package net.pistonmaster.pistonqueue.shared;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class StorageTool {
    private static Path dataDirectory;
    private static ConfigurationNode dataConfig;
    private static Path dataFile;

    private StorageTool() {
    }

    /**
     * Shadow-ban a player!
     *
     * @param player The player to shadow-ban.
     * @param date   The date when he will be unbanned.
     * @return true if player got shadow-banned and if already shadow-banned false.
     */
    public static boolean shadowBanPlayer(UUID player, Date date) {
        manageBan(player);

        if (dataConfig.node(player.toString()).virtual()) {
            try {
                dataConfig.node(player.toString()).set(date.toString());
            } catch (SerializationException e) {
                e.printStackTrace();
            }

            saveData();

            return true;
        } else {
            return false;
        }
    }

    /**
     * Un-shadow-ban a player!
     *
     * @param player The player to un-shadow-ban.
     * @return true if a player got un-shadow-banned and false if he wasn't shadow-banned.
     */
    public static boolean unShadowBanPlayer(UUID player) {
        if (!dataConfig.node(player.toString()).virtual()) {
            try {
                dataConfig.node(player.toString()).set(null);
            } catch (SerializationException e) {
                e.printStackTrace();
            }

            saveData();

            return true;
        } else {
            return false;
        }
    }

    public static boolean isShadowBanned(UUID player) {
        manageBan(player);

        return !dataConfig.node(player.toString()).virtual();
    }

    private static void manageBan(UUID player) {
        Date now = new Date();

        if (!dataConfig.node(player.toString()).virtual()) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", new Locale("us"));

            try {
                Date date = sdf.parse(dataConfig.node(player.toString()).getString());

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
            dataConfig = YamlConfigurationLoader.builder().path(dataFile).build().load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveData() {
        generateFile();

        try {
            YamlConfigurationLoader.builder().path(dataFile).build().save(dataConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateFile() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            if (!Files.exists(dataFile)) {
                Files.createFile(dataFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setupTool(Path dataDirectory) {
        StorageTool.dataDirectory = dataDirectory;
        StorageTool.dataFile = dataDirectory.resolve("data.yml");

        loadData();
    }
}
