package Leees.Bungee.Queue.Bukkit;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class Main extends JavaPlugin implements Listener {

    public boolean forceLocation = true;

    public String forcedWorldName = "world_the_end";
    public int forcedX = 0;
    public int forcedY = 200;
    public int forcedZ = 0;

    public boolean hidePlayers = true;
    public boolean disableChat = true;
    public boolean disableCmd = true;
    public boolean restrictMovement = true;
    public boolean forceGamemode = true;
    public String forcedGamemode = "spectator"; // spectator

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.forceLocation = this.getConfig().getBoolean("forceLocation");
        this.forcedWorldName = this.getConfig().getString("forcedWorldName");
        this.forcedX = this.getConfig().getInt("forcedX");
        this.forcedY = this.getConfig().getInt("forcedY");
        this.forcedZ = this.getConfig().getInt("forcedZ");
        this.hidePlayers = this.getConfig().getBoolean("hidePlayers");
        this.restrictMovement = this.getConfig().getBoolean("restrictMovement");
        this.forceGamemode = this.getConfig().getBoolean("forceGamemode");
        this.disableChat = this.getConfig().getBoolean("disableChat");
        this.disableCmd = this.getConfig().getBoolean("disableCmd");
        this.forcedGamemode = this.getConfig().getString("forcedGamemode");
        setGameRule();
        this.getServer().getPluginManager().registerEvents(this, this);
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (isExcluded(e.getPlayer())) {
            e.getPlayer().sendMessage("\2476Due to your permissions, you've been excluded from the queue movement and gamemode restrictions.");
            return;
        }
        if (!forceLocation) return;
        e.getPlayer().teleport(generateForcedLocation());
    }

    @EventHandler
    public void onPlayerJoin$0(PlayerJoinEvent e) {
        if (!hidePlayers) return;
        for (Player onlinePlayer : getServer().getOnlinePlayers()) {
            e.getPlayer().hidePlayer(this, onlinePlayer);
            onlinePlayer.hidePlayer(this, e.getPlayer());
            e.setJoinMessage("");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!hidePlayers) return;
        e.setQuitMessage("");
    }

    public void setGameRule() {
        if (!hidePlayers) return;
        for (World world : this.getServer().getWorlds()) {
            world.setGameRuleValue("announceAdvancements", "false");
        }
        this.getLogger().log(Level.INFO, "Gamerule announceAdvancements was set to false because hidePlayers was true.");
    }

    @EventHandler
    public void onPlayerJoin$1(PlayerJoinEvent e) {
        if (!forceGamemode) return;
        if (isExcluded(e.getPlayer())) return;
        e.getPlayer().setGameMode(GameMode.valueOf(forcedGamemode.toUpperCase()));
    }

    @EventHandler
    public void onPlayerSpawn(PlayerRespawnEvent e) {
        if (!forceLocation) return;
        if (isExcluded(e.getPlayer())) return;
        e.setRespawnLocation(generateForcedLocation());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (disableChat) e.setCancelled(true);
    }

    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (disableCmd) e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!restrictMovement) return;
        if (isExcluded(e.getPlayer())) return;
        e.setCancelled(true);
    }

    private boolean isExcluded(Player player) {
        return (player.isOp() || player.hasPermission("queue.admin"));
    }

    private Location generateForcedLocation() {
        if (getServer().getWorld(forcedWorldName) == null) {
            this.getLogger().log(Level.SEVERE, "Invalid forcedWorldName!! Check the configuration.");
            return null;
        }
        return new Location(getServer().getWorld(forcedWorldName), forcedX, forcedY, forcedZ);
    }
}