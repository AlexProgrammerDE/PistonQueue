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

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class ServerListener implements Listener {
    private final PistonQueueBukkit plugin;

    @EventHandler
    public void onPlayerJoin1(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if (isExcluded(player)) {
            player.sendMessage(ChatColor.GOLD + "Due to your permissions, you've been excluded from the queue movement and gamemode restrictions.");

            return;
        }

        if (plugin.isForceGamemode())
            player.setGameMode(GameMode.valueOf(plugin.getForcedGamemode().toUpperCase()));

        if (plugin.isHidePlayers())
            plugin.getServer().getOnlinePlayers().forEach(onlinePlayer -> {
                player.hidePlayer(plugin, onlinePlayer);
                onlinePlayer.hidePlayer(plugin, e.getPlayer());

                e.setJoinMessage(null);
            });

        if (plugin.isTeam()) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

            Team team = scoreboard.registerNewTeam(plugin.getTeamName()
                    .replace("%player_name%", player.getName())
                    .replace("%random%", String.valueOf(getRandomNumberUsingNextInt(-9999, 9999))));
            team.setCanSeeFriendlyInvisibles(false);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

            player.setScoreboard(scoreboard);

            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> team.addEntry(player.getName()), 20L);
        }

        if (plugin.isForceLocation())
            player.teleport(Objects.requireNonNull(generateForcedLocation()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (plugin.isHidePlayers())
            e.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerSpawn(PlayerRespawnEvent e) {
        if (plugin.isForceLocation() && !isExcluded(e.getPlayer()))
            e.setRespawnLocation(Objects.requireNonNull(generateForcedLocation()));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (plugin.isDisableChat()) e.setCancelled(true);
    }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (plugin.isDisableCmd()) e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (plugin.isRestrictMovement() && !isExcluded(e.getPlayer()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.isProtocolLib() && plugin.isDisableDebug()) {
            ProtocolLibWrapper.removeDebug(e.getPlayer());
        }
    }

    private boolean isExcluded(Player player) {
        return (player.isOp() || player.hasPermission("queue.admin"));
    }

    private Location generateForcedLocation() {
        if (plugin.getServer().getWorld(plugin.getForcedWorldName()) == null) {
            plugin.getLogger().log(Level.SEVERE, "Invalid forcedWorldName!! Check the configuration.");

            return null;
        }

        return new Location(plugin.getServer().getWorld(plugin.getForcedWorldName()), plugin.getForcedX(), plugin.getForcedY(), plugin.getForcedZ());
    }

    public int getRandomNumberUsingNextInt(int min, int max) {
        return new Random().nextInt(max - min) + min;
    }
}
