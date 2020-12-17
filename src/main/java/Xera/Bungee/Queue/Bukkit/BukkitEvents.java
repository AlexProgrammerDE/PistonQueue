package Xera.Bungee.Queue.Bukkit;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.Objects;
import java.util.logging.Level;

public final class BukkitEvents implements Listener {
    private final XeraBungeeQueueBukkit plugin;

    public BukkitEvents(XeraBungeeQueueBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (isExcluded(e.getPlayer())) {
            e.getPlayer().sendMessage("\2476Due to your permissions, you've been excluded from the queue movement and gamemode restrictions.");

            return;
        }

        if (!plugin.forceLocation) return;

        e.getPlayer().teleport(Objects.requireNonNull(generateForcedLocation()));
    }

    @EventHandler
    public void onPlayerJoin$0(PlayerJoinEvent e) {
        if (!plugin.hidePlayers) return;

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            e.getPlayer().hidePlayer(plugin, onlinePlayer);
            onlinePlayer.hidePlayer(plugin, e.getPlayer());

            e.setJoinMessage(null);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!plugin.hidePlayers) return;

        e.setQuitMessage(null);
    }

    @EventHandler
    public void onPlayerJoin$1(PlayerJoinEvent e) {
        if (!plugin.forceGamemode) return;
        if (isExcluded(e.getPlayer())) return;

        e.getPlayer().setGameMode(GameMode.valueOf(plugin.forcedGamemode.toUpperCase()));
    }

    @EventHandler
    public void onPlayerSpawn(PlayerRespawnEvent e) {
        if (!plugin.forceLocation) return;
        if (isExcluded(e.getPlayer())) return;

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
        if (!plugin.restrictMovement || isExcluded(e.getPlayer())) return;

        e.setCancelled(true);
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
