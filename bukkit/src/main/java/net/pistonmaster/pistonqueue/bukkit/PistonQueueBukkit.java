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

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.pistonmaster.pistonutils.update.GitHubUpdateChecker;
import net.pistonmaster.pistonutils.update.SemanticVersion;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Logger;

@Getter
public final class PistonQueueBukkit extends JavaPlugin {
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
        saveDefaultConfig();

        forceLocation = getConfig().getBoolean("forceLocation");
        forcedWorldName = getConfig().getString("forcedWorldName");
        forcedX = getConfig().getInt("forcedX");
        forcedY = getConfig().getInt("forcedY");
        forcedZ = getConfig().getInt("forcedZ");
        hidePlayers = getConfig().getBoolean("hidePlayers");
        restrictMovement = getConfig().getBoolean("restrictMovement");
        forceGamemode = getConfig().getBoolean("forceGamemode");
        disableChat = getConfig().getBoolean("disableChat");
        disableCmd = getConfig().getBoolean("disableCmd");
        forcedGamemode = getConfig().getString("forcedGamemode");
        team = getConfig().getBoolean("team");
        teamName = getConfig().getString("teamName");

        preventExperience = getConfig().getBoolean("preventExperience");
        preventDamage = getConfig().getBoolean("preventDamage");
        preventHunger = getConfig().getBoolean("preventHunger");

        disableDebug = getConfig().getBoolean("disableDebug");

        noChunkPackets = getConfig().getBoolean("noChunkPackets");
        noTimePackets = getConfig().getBoolean("noTimePackets");
        noHealthPackets = getConfig().getBoolean("noHealthPackets");
        noAdvancementPackets = getConfig().getBoolean("noAdvancementPackets");
        noExperiencePackets = getConfig().getBoolean("noExperiencePackets");
        showHeadPacket = getConfig().getBoolean("showHeadPacket");

        playXP = getConfig().getBoolean("playXP");

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
                log.info(ChatColor.BLUE + "You're up to date!");
            } else {
                log.info(ChatColor.RED + "There is an update available!");
                log.info(ChatColor.RED + "Current version: " + currentVersionString + " New version: " + gitHubVersion);
                log.info(ChatColor.RED + "Download it at: https://github.com/AlexProgrammerDE/PistonQueue/releases");
            }
        } catch (IOException e) {
            log.severe("Could not check for updates!");
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}
