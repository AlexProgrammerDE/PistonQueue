package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public final class ChatUtils {
    public static String parseToString(String str) {
        return ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(str));
    }

    public static BaseComponent[] parseToComponent(String str) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', XeraBungeeQueue.parseText(str)));
    }
}
