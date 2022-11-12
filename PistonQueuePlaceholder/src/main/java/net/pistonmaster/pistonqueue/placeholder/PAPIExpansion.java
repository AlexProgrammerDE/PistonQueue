package net.pistonmaster.pistonqueue.placeholder;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@RequiredArgsConstructor
public final class PAPIExpansion extends PlaceholderExpansion {
    private final PistonQueuePlaceholder plugin;

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getAuthor() {
        return "AlexProgrammerDE";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "PistonQueue";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        for (Map.Entry<String, Integer> entry : plugin.getOnlineQueue().entrySet()) {
            if (identifier.equalsIgnoreCase("online_queue_" + entry.getKey())) {
                return String.valueOf(entry.getValue());
            }
        }

        for (Map.Entry<String, Integer> entry : plugin.getOnlineTarget().entrySet()) {
            if (identifier.equalsIgnoreCase("online_target_" + entry.getKey())) {
                return String.valueOf(entry.getValue());
            }
        }

        return null;
    }
}
