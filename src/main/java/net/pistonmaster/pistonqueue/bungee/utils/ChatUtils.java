/*
 * #%L
 * PistonQueue
 * %%
 * Copyright (C) 2021 AlexProgrammerDE
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.pistonmaster.pistonqueue.bungee.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.pistonmaster.pistonqueue.utils.Config;
import net.pistonmaster.pistonqueue.utils.QueueAPI;

import java.time.Duration;
import java.util.List;

public final class ChatUtils {
    private ChatUtils() {
    }

    public static void sendMessage(ProxiedPlayer p, String str) {
        sendMessage(ChatMessageType.CHAT, p, str);
    }

    public static void sendMessage(ChatMessageType type, ProxiedPlayer p, String str) {
        if (!str.equalsIgnoreCase("/")) {
            p.sendMessage(type, parseToComponent(str));
        }
    }

    public static String parseToString(String str) {
        return ChatColor.translateAlternateColorCodes('&', parseText(str));
    }

    public static BaseComponent[] parseToComponent(String str) {
        return TextComponent.fromLegacyText(parseToString(str));
    }

    private static String parseText(String text) {
        text = text.replace("%server%", Config.SERVERNAME);
        text = text.replace("%veteran%", String.valueOf(QueueAPI.getVeteranSize()));
        text = text.replace("%priority%", String.valueOf(QueueAPI.getPrioritySize()));
        text = text.replace("%regular%", String.valueOf(QueueAPI.getRegularSize()));
        text = text.replace("%position%", "None");
        text = text.replace("%wait%", "None");

        return text;
    }

    public static BaseComponent[] parseTab(List<String> tab) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < tab.size(); i++) {
            builder.append(ChatUtils.parseToString(tab.get(i)));

            if (i != (tab.size() - 1)) {
                builder.append("\n");
            }
        }

        return new ComponentBuilder(builder.toString()).create();
    }

    public static String formatDuration(String str, Duration duration, int position) {
        String format = String.format("%dh %dm", duration.toHours(), duration.toMinutes() % 60);

        if (duration.toHours() == 0)
            format = String.format("%dm", duration.toMinutes() == 0 ? 1 : duration.toMinutes());

        return str.replace("%position%", String.valueOf(position)).replace("%wait%", format);
    }
}
