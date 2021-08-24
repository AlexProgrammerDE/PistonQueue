package net.pistonmaster.pistonqueue.utils;

import java.time.Duration;

public class SharedChatUtils {
    private SharedChatUtils() {
    }

    public static String formatDuration(String str, Duration duration, int position) {
        String format = String.format("%dh %dm", duration.toHours(), duration.toMinutes() % 60);

        if (duration.toHours() == 0)
            format = String.format("%dm", duration.toMinutes() == 0 ? 1 : duration.toMinutes());

        return str.replace("%position%", String.valueOf(position)).replace("%wait%", format);
    }

    public static String parseText(String text) {
        text = text.replace("%server%", Config.SERVERNAME);
        text = text.replace("%veteran%", String.valueOf(QueueAPI.getVeteranSize()));
        text = text.replace("%priority%", String.valueOf(QueueAPI.getPrioritySize()));
        text = text.replace("%regular%", String.valueOf(QueueAPI.getRegularSize()));
        text = text.replace("%position%", "None");
        text = text.replace("%wait%", "None");

        return text;
    }
}
