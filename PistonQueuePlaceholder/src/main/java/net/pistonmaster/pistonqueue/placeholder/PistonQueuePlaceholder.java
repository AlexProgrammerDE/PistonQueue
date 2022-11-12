package net.pistonmaster.pistonqueue.placeholder;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Getter
public final class PistonQueuePlaceholder extends JavaPlugin implements PluginMessageListener {
    private final Map<String, Integer> onlineQueue = new ConcurrentHashMap<>();
    private final Map<String, Integer> onlineTarget = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Logger log = getLogger();

        checkIfBungee();

        log.info(ChatColor.BLUE + "Registering messaging channel");
        getServer().getMessenger().registerIncomingPluginChannel(this, "piston:queue", this);

        log.info(ChatColor.BLUE + "Registering PAPI expansion");
        new PAPIExpansion(this).register();

        log.info(ChatColor.BLUE + "Successfully enabled!");
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte[] bytes) {
        if (!channel.equalsIgnoreCase("piston:queue")) {
            return;
        }

        @SuppressWarnings({"UnstableApiUsage"})
        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String subChannel = in.readUTF();

        if (subChannel.equalsIgnoreCase("onlineQueue")) {
            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                String queue = in.readUTF();
                int online = in.readInt();

                onlineQueue.put(queue, online);
            }
        } else if (subChannel.equalsIgnoreCase("onlineTarget")) {
            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                String queue = in.readUTF();
                int online = in.readInt();

                onlineTarget.put(queue, online);
            }
        }
    }

    private void checkIfBungee() {
        if (!PaperLib.isSpigot()) {
            getLogger().severe(ChatColor.RED + "You probably run CraftBukkit. Update at least to spigot for this plugin to work!");
            getLogger().severe(ChatColor.RED + "Plugin disabled!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}
