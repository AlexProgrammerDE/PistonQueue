package net.pistonmaster.pistonqueue.placeholder;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import net.pistonmaster.pistonutils.update.GitHubUpdateChecker;
import net.pistonmaster.pistonutils.update.SemanticVersion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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

    log.info(ChatColor.BLUE + "Checking for a newer version");
    try {
      String currentVersionString = this.getDescription().getVersion();
      SemanticVersion gitHubVersion = new GitHubUpdateChecker()
        .getVersion("https://api.github.com/repos/AlexProgrammerDE/PistonQueue/releases/latest");
      SemanticVersion currentVersion = SemanticVersion.fromString(currentVersionString);

      if (gitHubVersion.isNewerThan(currentVersion)) {
        log.info(ChatColor.RED + "There is an update available!");
        log.info(ChatColor.RED + "Current version: " + currentVersionString + " New version: " + gitHubVersion);
        log.info(ChatColor.RED + "Download it at: https://modrinth.com/plugin/pistonqueue");
      } else {
        log.info(ChatColor.BLUE + "You're up to date!");
      }
    } catch (IOException e) {
      log.severe("Could not check for updates!");
      e.printStackTrace();
    }

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
    if (!isSpigot()) {
      getLogger().severe(ChatColor.RED + "You probably run CraftBukkit. Update at least to Spigot for this plugin to work!");
      getLogger().severe(ChatColor.RED + "Plugin disabled!");
      getServer().getPluginManager().disablePlugin(this);
    }
  }

  private boolean isSpigot() {
    try {
      Class.forName("org.spigotmc.SpigotConfig");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
