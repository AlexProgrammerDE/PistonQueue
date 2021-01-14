package ca.xera.bungee.queue.bungee.hooks;

import ca.xera.bungee.queue.bungee.QueueAPI;
import me.alexprogrammerde.pistonmotd.api.PlaceholderParser;

public class PistonMOTDPlaceholder implements PlaceholderParser {
    @Override
    public String parseString(String s) {
        return s.replace("%xerabungeequeue_regular%", String.valueOf(QueueAPI.getRegularSize()))
                .replace("%xerabungeequeue_priority%", String.valueOf(QueueAPI.getPrioritySize()))
                .replace("%xerabungeequeue_veteran%", String.valueOf(QueueAPI.getVeteranSize()));
    }
}
