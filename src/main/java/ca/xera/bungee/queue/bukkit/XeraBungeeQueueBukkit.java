package ca.xera.bungee.queue.bukkit;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

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
    protected boolean protocolLib = false;
    protected boolean disableDebug = true;

    @Override
    public void onEnable() {
        getLogger().info(ChatColor.BLUE + "XeraBungeeQueue V" + getDescription().getVersion());

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

        getLogger().info(ChatColor.BLUE + "Preparing server");
        if (hidePlayers) {
            for (World world : getServer().getWorlds()) {
                world.setGameRuleValue("announceAdvancements", "false");
            }

            getLogger().info(ChatColor.BLUE + "Gamerule announceAdvancements was set to false because hidePlayers was true.");
        }

        getLogger().info(ChatColor.BLUE + "Looking for hooks");
        if (getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().info(ChatColor.BLUE + "Hooked into ProtocolLib");
            protocolLib = true;

            ProtocolLibWrapper.setupProtocolLib(this);
        } else {
            getLogger().info(ChatColor.YELLOW + "It is recommended to install Protocol");
        }

        getLogger().info(ChatColor.BLUE + "Registering listeners");
        getServer().getPluginManager().registerEvents(new ServerListener(this), this);
    }
}