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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

@RequiredArgsConstructor
public final class ServerListener implements Listener {
  private final PistonQueueBukkit plugin;

  @EventHandler(ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (isExcluded(player)) {
      player.sendMessage(ChatColor.GOLD + "Due to your permissions, you've been excluded from the queue restrictions.");

      return;
    }

    if (plugin.isForceGamemode()) {
      player.setGameMode(GameMode.valueOf(plugin.getForcedGamemode().toUpperCase(Locale.ROOT)));
    }

    if (plugin.isHidePlayers()) {
      plugin.getServer().getOnlinePlayers().forEach(onlinePlayer -> {
        player.hidePlayer(plugin, onlinePlayer);
        onlinePlayer.hidePlayer(plugin, event.getPlayer());

        event.setJoinMessage(null);
      });
    }

    ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (plugin.isTeam() && manager != null) {
      Scoreboard scoreboard = manager.getNewScoreboard();

      Team team = scoreboard.registerNewTeam(plugin.getTeamName()
        .replace("%player_name%", player.getName())
        .replace("%random%", String.valueOf(getRandomNumberUsingNextInt(-9999, 9999))));
      team.setCanSeeFriendlyInvisibles(false);
      team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

      player.setScoreboard(scoreboard);

      Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> team.addEntry(player.getName()), 20L);
    }

    if (plugin.isForceLocation()) {
      player.teleport(Objects.requireNonNull(generateForcedLocation()));
    }

    if (plugin.isProtocolLib() && plugin.isDisableDebug()) {
      ProtocolLibWrapper.removeDebug(player);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (plugin.isHidePlayers()) {
      event.setQuitMessage(null);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    if (plugin.isForceLocation() && !isExcluded(event.getPlayer())) {
      event.setRespawnLocation(Objects.requireNonNull(generateForcedLocation()));
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerHunger(FoodLevelChangeEvent event) {
    if (plugin.isPreventHunger() && !isExcluded(event.getEntity())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerHunger(PlayerExpChangeEvent event) {
    if (plugin.isPreventHunger() && !isExcluded(event.getPlayer())) {
      event.setAmount(0);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (plugin.isPreventDamage() && !isExcluded(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageByBlockEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (plugin.isPreventDamage() && !isExcluded(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (plugin.isPreventDamage() && !isExcluded(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onChat(AsyncPlayerChatEvent event) {
    if (plugin.isDisableChat()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onCmd(PlayerCommandPreprocessEvent event) {
    if (plugin.isDisableCmd()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onMove(PlayerMoveEvent event) {
    if (plugin.isRestrictMovement() && !isExcluded(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  private boolean isExcluded(HumanEntity player) {
    return player.hasPermission("queue.admin");
  }

  private Location generateForcedLocation() {
    if (plugin.getServer().getWorld(plugin.getForcedWorldName()) == null) {
      plugin.getLogger().log(Level.SEVERE, "Invalid forcedWorldName!! Check the configuration.");

      return null;
    }

    return new Location(plugin.getServer().getWorld(plugin.getForcedWorldName()), plugin.getForcedX(), plugin.getForcedY(), plugin.getForcedZ());
  }

  public int getRandomNumberUsingNextInt(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max);
  }
}
