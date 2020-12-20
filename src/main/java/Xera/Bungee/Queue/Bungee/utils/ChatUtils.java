package Xera.Bungee.Queue.Bungee.utils;

import Xera.Bungee.Queue.Bungee.QueueAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public final class ChatUtils {
    public static String parseToString(String str) {
        return ChatColor.translateAlternateColorCodes('&', parseText(str));
    }

    public static BaseComponent[] parseToComponent(String str) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', parseText(str)));
    }

    private static String parseText(String text) {
        String returnedText = text;

        returnedText = returnedText.replaceAll("%regular%", String.valueOf(QueueAPI.getRegularSize()));
        returnedText = returnedText.replaceAll("%priority%", String.valueOf(QueueAPI.getPrioritySize()));
        returnedText = returnedText.replaceAll("%veteran%", String.valueOf(QueueAPI.getVeteranSize()));

        return returnedText;
    }
}
