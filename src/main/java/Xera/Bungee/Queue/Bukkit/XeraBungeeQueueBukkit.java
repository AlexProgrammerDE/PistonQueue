package Xera.Bungee.Queue.Bukkit;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class XeraBungeeQueueBukkit extends JavaPlugin {

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
        
        setGameRule();
        
        getServer().getPluginManager().registerEvents(new BukkitEvents(this), this);
    }
    
    @Override
    public void onDisable() {

    }
    
    public void setGameRule() {
        if (hidePlayers) {
            for (World world : getServer().getWorlds()) {
                world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            }

            getLogger().log(Level.INFO, "Gamerule announceAdvancements was set to false because hidePlayers was true.");
        }
    }
}