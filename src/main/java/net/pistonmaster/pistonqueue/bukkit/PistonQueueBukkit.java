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
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

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
    private boolean protocolLib;
    private boolean disableDebug;
    private boolean team;
    private String teamName;
    private boolean noChunk;
    private boolean noTime;
    private boolean noHealth;
    private boolean noAdvancements;
    private boolean noExperience;
    private boolean showHead;
    private boolean playXP;

    @Override
    public void onEnable() {
        getLogger().info(ChatColor.BLUE + "PistonQueue V" + getDescription().getVersion());

        getLogger().info(ChatColor.BLUE + "Loading config");
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
        disableDebug = getConfig().getBoolean("disableDebug");
        team = getConfig().getBoolean("team");
        teamName = getConfig().getString("teamName");
        noChunk = getConfig().getBoolean("noChunk");
        noTime = getConfig().getBoolean("noTime");
        noHealth = getConfig().getBoolean("noHealth");
        noAdvancements = getConfig().getBoolean("noAdvancements");
        noExperience = getConfig().getBoolean("noExperience");
        showHead = getConfig().getBoolean("showHead");
        playXP = getConfig().getBoolean("playXP");

        getLogger().info(ChatColor.BLUE + "Preparing server");
        if (hidePlayers) {
            for (World world : getServer().getWorlds()) {
                world.setGameRuleValue("announceAdvancements", "false");
            }

            getLogger().info(ChatColor.BLUE + "Game-rule announceAdvancements was set to false because hidePlayers was true.");
        }

        getLogger().info(ChatColor.BLUE + "Looking for hooks");
        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().info(ChatColor.BLUE + "Hooked into ProtocolLib");
            protocolLib = true;

            ProtocolLibWrapper.setupProtocolLib(this);
        } else {
            getLogger().info(ChatColor.YELLOW + "It is recommended to install ProtocolLib");
        }

        getLogger().info(ChatColor.BLUE + "Registering listeners");
        getServer().getPluginManager().registerEvents(new ServerListener(this), this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "piston:queue", new QueuePluginMessageListener(this));
    }
}
