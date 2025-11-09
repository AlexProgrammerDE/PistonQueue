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
package net.pistonmaster.pistonqueue.shared.utils;

import de.exlll.configlib.YamlConfigurations;
import net.pistonmaster.pistonqueue.shared.config.StorageData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StorageTool {
  private static final Logger LOGGER = Logger.getLogger(StorageTool.class.getName());
  private static Path dataDirectory;
  private static StorageData dataConfig;
  private static Path dataFile;

  private StorageTool() {
  }

  /**
   * Shadow-ban a player!
   *
   * @param playerName The player to shadow-ban.
   * @param date       The date when he will be unbanned.
   * @return true if player got shadow-banned and if already shadow-banned false.
   */
  public static boolean shadowBanPlayer(String playerName, Date date) {
    playerName = playerName.toLowerCase(Locale.ROOT);
    manageBan(playerName);

    Map<String, String> bans = dataConfig.getMutableBans();
    if (!bans.containsKey(playerName)) {
      bans.put(playerName, date.toInstant().toString());
      saveData();

      return true;
    } else {
      return false;
    }
  }

  /**
   * Un-shadow-ban a player!
   *
   * @param playerName The player to un-shadow-ban.
   * @return true if a player got un-shadow-banned and false if he wasn't shadow-banned.
   */
  public static boolean unShadowBanPlayer(String playerName) {
    playerName = playerName.toLowerCase(Locale.ROOT);
    if (dataConfig.getMutableBans().remove(playerName) != null) {
      saveData();

      return true;
    } else {
      return false;
    }
  }

  public static boolean isShadowBanned(String playerName) {
    playerName = playerName.toLowerCase(Locale.ROOT);
    manageBan(playerName);

    return dataConfig.getMutableBans().containsKey(playerName);
  }

  private static void manageBan(String playerName) {
    playerName = playerName.toLowerCase(Locale.ROOT);
    Instant now = Instant.now();

    if (dataConfig.getMutableBans().containsKey(playerName)) {
      Instant expiresAt = parseExpiryInstant(dataConfig.getMutableBans().get(playerName));
      if (expiresAt != null && !now.isBefore(expiresAt)) {
        unShadowBanPlayer(playerName);
      }
    }
  }

  private static void loadData() {
    generateFile();
    convertLegacyDataIfNeeded();
    ensureFileInitialized();
    dataConfig = YamlConfigurations.update(dataFile, StorageData.class);
  }

  private static void saveData() {
    generateFile();
    YamlConfigurations.save(dataFile, StorageData.class, dataConfig);
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
      LOGGER.log(Level.SEVERE, "Failed to create data file", e);
    }
  }

  public static void setupTool(Path dataDirectory) {
    StorageTool.dataDirectory = dataDirectory;
    StorageTool.dataFile = dataDirectory.resolve("data.yml");

    loadData();
  }

  private static void convertLegacyDataIfNeeded() {
    try {
      if (!Files.exists(dataFile)) {
        return;
      }
      List<String> lines = Files.readAllLines(dataFile);
      boolean alreadyNewFormat = lines.stream()
        .map(String::trim)
        .anyMatch(line -> line.startsWith("bans:"));
      if (alreadyNewFormat || lines.isEmpty()) {
        return;
      }

      Map<String, String> legacyEntries = new LinkedHashMap<>();
      for (String rawLine : lines) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        int colonIndex = line.indexOf(':');
        if (colonIndex < 0) {
          continue;
        }
        String key = line.substring(0, colonIndex).trim();
        String value = line.substring(colonIndex + 1).trim();
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
          value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
          value = value.substring(1, value.length() - 1);
        }
        if (!key.isEmpty()) {
          legacyEntries.put(key, value);
        }
      }

      if (legacyEntries.isEmpty()) {
        return;
      }

      StorageData legacyData = new StorageData();
      legacyData.getMutableBans().putAll(legacyEntries);
      YamlConfigurations.save(dataFile, StorageData.class, legacyData);
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to convert legacy data", e);
    }
  }

  private static void ensureFileInitialized() {
    try {
      if (Files.size(dataFile) == 0) {
        YamlConfigurations.save(dataFile, StorageData.class, new StorageData());
      }
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize storage file", e);
    }
  }

  private static Instant parseExpiryInstant(String value) {
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      try {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.of("en"));
        Date parsed = sdf.parse(value);
        return parsed.toInstant();
      } catch (ParseException e) {
        LOGGER.log(Level.WARNING, "Failed to parse shadow ban expiry value \"" + value + '"', e);
        return null;
      }
    }
  }
}
