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

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.Objects;
import java.util.logging.Level;

public final class ServerListener implements Listener {
    private final PistonQueueBukkit plugin;

    public ServerListener(PistonQueueBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin1(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if (isExcluded(player)) {
            player.sendMessage(ChatColor.GOLD + "Due to your permissions, you've been excluded from the queue movement and gamemode restrictions.");

            return;
        }

        if (plugin.forceGamemode)
            player.setGameMode(GameMode.valueOf(plugin.forcedGamemode.toUpperCase()));

        if (plugin.forceLocation)
            player.teleport(Objects.requireNonNull(generateForcedLocation()));

        if (plugin.hidePlayers)
            plugin.getServer().getOnlinePlayers().forEach(onlinePlayer -> {
                player.hidePlayer(plugin, onlinePlayer);
                onlinePlayer.hidePlayer(plugin, e.getPlayer());

                e.setJoinMessage(null);
            });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (plugin.hidePlayers)
            e.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerSpawn(PlayerRespawnEvent e) {
        if (plugin.forceLocation && !isExcluded(e.getPlayer()))
            e.setRespawnLocation(Objects.requireNonNull(generateForcedLocation()));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (plugin.disableChat) e.setCancelled(true);
    }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (plugin.disableCmd) e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (plugin.restrictMovement && !isExcluded(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.protocolLib && plugin.disableDebug) {
            ProtocolLibWrapper.removeDebug(e.getPlayer());
        }
    }

    private boolean isExcluded(Player player) {
        return (player.isOp() || player.hasPermission("queue.admin"));
    }

    private Location generateForcedLocation() {
        if (plugin.getServer().getWorld(plugin.forcedWorldName) == null) {
            plugin.getLogger().log(Level.SEVERE, "Invalid forcedWorldName!! Check the configuration.");

            return null;
        }

        return new Location(plugin.getServer().getWorld(plugin.forcedWorldName), plugin.forcedX, plugin.forcedY, plugin.forcedZ);
    }
}
