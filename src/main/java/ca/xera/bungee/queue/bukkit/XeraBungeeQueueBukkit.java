package ca.xera.bungee.queue.bukkit;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class XeraBungeeQueueBukkit extends JavaPlugin {

    protected boolean forceLocation = true;

    protected String forcedWorldName = "world_the_end";
    protected int forcedX = 0;
    protected int forcedY = 200;
    protected int forcedZ = 0;

    protected boolean hidePlayers = true;
    protected boolean disableChat = true;
    protected boolean disableCmd = true;
    protected boolean restrictMovement = true;
    protected boolean forceGamemode = true;
    protected String forcedGamemode = "spectator"; // spectator

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

        getServer().getPluginManager().registerEvents(new ServerListener(this), this);
    }

    @Override
    public void onDisable() {
    }

    protected void setGameRule() {
        if (hidePlayers) {
            for (World world : getServer().getWorlds()) {
                world.setGameRuleValue("announceAdvancements", "false");
            }

            getLogger().log(Level.INFO, "Gamerule announceAdvancements was set to false because hidePlayers was true.");
        }
    }
}