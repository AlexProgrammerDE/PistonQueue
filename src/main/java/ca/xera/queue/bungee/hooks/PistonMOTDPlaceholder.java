package ca.xera.queue.bungee.hooks;

import ca.xera.queue.bungee.QueueAPI;
import me.alexprogrammerde.pistonmotd.api.PlaceholderParser;
import me.alexprogrammerde.pistonmotd.api.PlaceholderUtil;

public class PistonMOTDPlaceholder implements PlaceholderParser {
    public PistonMOTDPlaceholder() {
        PlaceholderUtil.registerParser(this);
    }

    @Override
    public String parseString(String s) {
        return s.replace("%xerabungeequeue_regular%", String.valueOf(QueueAPI.getRegularSize()))
                .replace("%xerabungeequeue_priority%", String.valueOf(QueueAPI.getPrioritySize()))
                .replace("%xerabungeequeue_veteran%", String.valueOf(QueueAPI.getVeteranSize()));
    }
}
