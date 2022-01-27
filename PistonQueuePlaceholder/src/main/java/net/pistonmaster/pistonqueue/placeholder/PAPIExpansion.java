package net.pistonmaster.pistonqueue.placeholder;

import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

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
        if (identifier.equals("online_queue_regular")) {
            return String.valueOf(plugin.getOnlineQueueRegular());
        }

        if (identifier.equals("online_queue_priority")) {
            return String.valueOf(plugin.getOnlineQueuePriority());
        }

        if (identifier.equals("online_queue_veteran")) {
            return String.valueOf(plugin.getOnlineQueueVeteran());
        }

        if (identifier.equals("online_main_regular")) {
            return String.valueOf(plugin.getOnlineMainRegular());
        }

        if (identifier.equals("online_main_priority")) {
            return String.valueOf(plugin.getOnlineMainPriority());
        }

        if (identifier.equals("online_main_veteran")) {
            return String.valueOf(plugin.getOnlineMainVeteran());
        }

        return null;
    }
}
