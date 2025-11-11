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
package net.pistonmaster.pistonqueue.bukkit;

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurations;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonqueue.bukkit.config.BukkitConfig;
import net.pistonmaster.pistonutils.update.GitHubUpdateChecker;
import net.pistonmaster.pistonutils.update.SemanticVersion;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public final class PistonQueueBukkit extends JavaPlugin {
  private final Set<String> warnedLegacyPaths = new HashSet<>();
  private BukkitConfig pluginConfig;
  private boolean forceLocation;

  private String forcedWorldName;
  private int forcedX;
  private int forcedY;
  private int forcedZ;

  private boolean hidePlayers;
  private boolean disableChat;
  private boolean disableCmd;
  private boolean restrictMovement;
  private boolean forceGamemode;
  private String forcedGamemode;

  private boolean team;
  private String teamName;

  private boolean preventExperience;
  private boolean preventDamage;
  private boolean preventHunger;

  private boolean protocolLib;
  private boolean disableDebug;

  private boolean noChunkPackets;
  private boolean noTimePackets;
  private boolean noHealthPackets;
  private boolean noAdvancementPackets;
  private boolean noExperiencePackets;
  private boolean showHeadPacket;

  private boolean playXP;

  @SuppressWarnings("deprecation")
  @Override
  public void onEnable() {
    Logger log = getLogger();
    log.info(ChatColor.BLUE + "PistonQueue V" + getDescription().getVersion());

    log.info(ChatColor.BLUE + "Loading config");
    try {
      pluginConfig = loadConfig();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load config", e);
    }

    applyConfig(pluginConfig);

    log.info(ChatColor.BLUE + "Preparing server");
    if (hidePlayers) {
      for (World world : getServer().getWorlds()) {
        world.setGameRuleValue("announceAdvancements", "false");
      }

      log.info(ChatColor.BLUE + "Game-rule announceAdvancements was set to false because hidePlayers was true.");
    }

    log.info(ChatColor.BLUE + "Looking for hooks");
    if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
      log.info(ChatColor.BLUE + "Hooked into ProtocolLib");
      protocolLib = true;

      ProtocolLibWrapper.setupProtocolLib(this);
    } else {
      log.info(ChatColor.YELLOW + "It is recommended to install ProtocolLib");
    }

    log.info(ChatColor.BLUE + "Registering listeners");
    getServer().getPluginManager().registerEvents(new ServerListener(this), this);
    getServer().getMessenger().registerIncomingPluginChannel(this, "piston:queue", new QueuePluginMessageListener(this));

    log.info(ChatColor.BLUE + "Checking for a newer version");
    try {
      String currentVersionString = this.getDescription().getVersion();
      SemanticVersion gitHubVersion = new GitHubUpdateChecker()
        .getVersion("https://api.github.com/repos/AlexProgrammerDE/PistonQueue/releases/latest");
      SemanticVersion currentVersion = SemanticVersion.fromString(currentVersionString);

      if (gitHubVersion.isNewerThan(currentVersion)) {
        log.info(ChatColor.RED + "There is an update available!");
        log.info(ChatColor.RED + "Current version: " + currentVersionString + " New version: " + gitHubVersion);
        log.info(ChatColor.RED + "Download it at: https://modrinth.com/plugin/pistonqueue");
      } else {
        log.info(ChatColor.BLUE + "You're up to date!");
      }
    } catch (IOException e) {
      log.log(Level.SEVERE, "Could not check for updates!", e);
    }
  }

  @Override
  public void onDisable() {
    this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
  }

  private BukkitConfig loadConfig() throws IOException {
    Path dataFolder = getDataFolder().toPath();
    if (Files.notExists(dataFolder)) {
      Files.createDirectories(dataFolder);
    }

    Path configFile = dataFolder.resolve("config.yml");
    BukkitConfig config = YamlConfigurations.update(
      configFile,
      BukkitConfig.class,
      builder -> builder.setNameFormatter(NameFormatters.IDENTITY)
    );

    if (applyLegacyOverrides(configFile.toFile(), config)) {
      YamlConfigurations.save(configFile, BukkitConfig.class, config);
    }

    return config;
  }

  private void applyConfig(BukkitConfig config) {
    forceLocation = config.location.enabled;
    forcedWorldName = config.location.world;
    forcedX = config.location.coordinates.x;
    forcedY = config.location.coordinates.y;
    forcedZ = config.location.coordinates.z;

    hidePlayers = config.visibility.hidePlayers;
    restrictMovement = config.visibility.restrictMovement;
    forceGamemode = config.visibility.forceGamemode.enabled;
    forcedGamemode = config.visibility.forceGamemode.mode;
    team = config.visibility.team.enabled;
    teamName = config.visibility.team.name;

    disableChat = config.communication.disableChat;
    disableCmd = config.communication.disableCommands;

    playXP = config.audio.playXpSound;

    preventExperience = config.protections.preventExperience;
    preventDamage = config.protections.preventDamage;
    preventHunger = config.protections.preventHunger;

    disableDebug = config.protocolLib.disableDebug;
    noChunkPackets = config.protocolLib.suppressPackets.chunk;
    noTimePackets = config.protocolLib.suppressPackets.time;
    noHealthPackets = config.protocolLib.suppressPackets.health;
    noAdvancementPackets = config.protocolLib.suppressPackets.advancement;
    noExperiencePackets = config.protocolLib.suppressPackets.experience;
    showHeadPacket = config.protocolLib.showFullHead;
  }

  private boolean applyLegacyOverrides(File file, BukkitConfig config) {
    if (file == null || !file.exists()) {
      return false;
    }

    YamlConfiguration legacy = YamlConfiguration.loadConfiguration(file);
    boolean updated = false;

    updated |= copyBoolean(legacy, "forceLocation", "location.enabled", value -> config.location.enabled = value);
    updated |= copyString(legacy, "forcedWorldName", "location.world", value -> config.location.world = value);
    updated |= copyInt(legacy, "forcedX", "location.coordinates.x", value -> config.location.coordinates.x = value);
    updated |= copyInt(legacy, "forcedY", "location.coordinates.y", value -> config.location.coordinates.y = value);
    updated |= copyInt(legacy, "forcedZ", "location.coordinates.z", value -> config.location.coordinates.z = value);

    updated |= copyBoolean(legacy, "hidePlayers", "visibility.hidePlayers", value -> config.visibility.hidePlayers = value);
    updated |= copyBoolean(legacy, "restrictMovement", "visibility.restrictMovement", value -> config.visibility.restrictMovement = value);
    updated |= copyBoolean(legacy, "forceGamemode", "visibility.forceGamemode.enabled", value -> config.visibility.forceGamemode.enabled = value);
    updated |= copyString(legacy, "forcedGamemode", "visibility.forceGamemode.mode", value -> config.visibility.forceGamemode.mode = value);
    updated |= copyBoolean(legacy, "team", "visibility.team.enabled", value -> config.visibility.team.enabled = value);
    updated |= copyString(legacy, "teamName", "visibility.team.name", value -> config.visibility.team.name = value);

    updated |= copyBoolean(legacy, "disableChat", "communication.disableChat", value -> config.communication.disableChat = value);
    updated |= copyBoolean(legacy, "disableCmd", "communication.disableCommands", value -> config.communication.disableCommands = value);

    updated |= copyBoolean(legacy, "playXP", "audio.playXpSound", value -> config.audio.playXpSound = value);

    updated |= copyBoolean(legacy, "preventExperience", "protections.preventExperience", value -> config.protections.preventExperience = value);
    updated |= copyBoolean(legacy, "preventDamage", "protections.preventDamage", value -> config.protections.preventDamage = value);
    updated |= copyBoolean(legacy, "preventHunger", "protections.preventHunger", value -> config.protections.preventHunger = value);

    updated |= copyBoolean(legacy, "disableDebug", "protocolLib.disableDebug", value -> config.protocolLib.disableDebug = value);
    updated |= copyBoolean(legacy, "noChunkPackets", "protocolLib.suppressPackets.chunk", value -> config.protocolLib.suppressPackets.chunk = value);
    updated |= copyBoolean(legacy, "noTimePackets", "protocolLib.suppressPackets.time", value -> config.protocolLib.suppressPackets.time = value);
    updated |= copyBoolean(legacy, "noHealthPackets", "protocolLib.suppressPackets.health", value -> config.protocolLib.suppressPackets.health = value);
    updated |= copyBoolean(legacy, "noAdvancementPackets", "protocolLib.suppressPackets.advancement", value -> config.protocolLib.suppressPackets.advancement = value);
    updated |= copyBoolean(legacy, "noExperiencePackets", "protocolLib.suppressPackets.experience", value -> config.protocolLib.suppressPackets.experience = value);
    updated |= copyBoolean(legacy, "showHeadPacket", "protocolLib.showFullHead", value -> config.protocolLib.showFullHead = value);

    return updated;
  }

  private boolean copyBoolean(YamlConfiguration legacy, String legacyPath, String newPath, Consumer<Boolean> setter) {
    if (!legacy.contains(legacyPath)) {
      return false;
    }

    setter.accept(legacy.getBoolean(legacyPath));
    logLegacyUsage(newPath, legacyPath);
    return true;
  }

  private boolean copyInt(YamlConfiguration legacy, String legacyPath, String newPath, IntConsumer setter) {
    if (!legacy.contains(legacyPath)) {
      return false;
    }

    setter.accept(legacy.getInt(legacyPath));
    logLegacyUsage(newPath, legacyPath);
    return true;
  }

  private boolean copyString(YamlConfiguration legacy, String legacyPath, String newPath, Consumer<String> setter) {
    if (!legacy.contains(legacyPath)) {
      return false;
    }

    setter.accept(legacy.getString(legacyPath));
    logLegacyUsage(newPath, legacyPath);
    return true;
  }

  private void logLegacyUsage(String newPath, String legacyPath) {
    if (warnedLegacyPaths.add(legacyPath)) {
      getLogger()
        .warning(String.format("Configuration option '%s' has moved to '%s'. Please update your config.", legacyPath, newPath));
    }
  }
}
