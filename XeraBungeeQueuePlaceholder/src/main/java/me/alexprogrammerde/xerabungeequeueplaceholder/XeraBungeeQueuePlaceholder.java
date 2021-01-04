package me.alexprogrammerde.xerabungeequeueplaceholder;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.papermc.lib.PaperLib;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public final class XeraBungeeQueuePlaceholder extends JavaPlugin implements PluginMessageListener {
    protected int regular = 0;
    protected int priority = 0;
    protected int veteran = 0;

    @Override
    public void onEnable() {
        Logger log = getLogger();

        checkIfBungee();

        log.info(ChatColor.BLUE + "Registering messaging channel");
        getServer().getMessenger().registerIncomingPluginChannel( this, "xera:bungeequeue", this );

        log.info(ChatColor.BLUE + "Registering PAPI expansion");
        new PAPIExpansion(this).register();

        log.info(ChatColor.BLUE + "Successfully enabled!");
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes) {
        if (!channel.equalsIgnoreCase("xera:bungeequeue")) {
            return;
        }

        @SuppressWarnings({"UnstableApiUsage"})
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();

        if (subChannel.equalsIgnoreCase("size")) {
            regular = in.readInt();
            priority = in.readInt();
            veteran = in.readInt();
        }
    }

    private void checkIfBungee() {
        if (!PaperLib.isSpigot()) {
            getLogger().severe(ChatColor.RED + "You probably run CraftBukkit... Please update atleast to spigot for this to work...");
            getLogger().severe(ChatColor.RED + "Plugin disabled!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
