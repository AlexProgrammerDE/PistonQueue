package net.pistonmaster.pistonqueue.bungee.utils;

import net.pistonmaster.pistonqueue.bungee.QueueAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public final class ChatUtils {
    private ChatUtils() {
    }

    public static String parseToString(String str) {
        return ChatColor.translateAlternateColorCodes('&', parseText(str));
    }

    public static BaseComponent[] parseToComponent(String str) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', parseText(str)));
    }

    private static String parseText(String text) {
        String returnedText = text;

        returnedText = returnedText.replace("%veteran%", String.valueOf(QueueAPI.getVeteranSize()));
        returnedText = returnedText.replace("%priority%", String.valueOf(QueueAPI.getPrioritySize()));
        returnedText = returnedText.replace("%regular%", String.valueOf(QueueAPI.getRegularSize()));

        return returnedText;
    }
}
