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
import net.pistonmaster.pistonqueue.shared.utils.MessageType;
import net.pistonmaster.pistonqueue.shared.SharedChatUtils;

import java.util.List;

public final class ChatUtils {
    private ChatUtils() {
    }

    public static void sendMessage(ProxiedPlayer p, String str) {
        sendMessage(MessageType.CHAT, p, str);
    }

    public static void sendMessage(MessageType type, ProxiedPlayer p, String str) {
        if (!str.equalsIgnoreCase("/")) {
            switch (type) {
                case CHAT:
                    p.sendMessage(ChatMessageType.CHAT, parseToComponent(str));
                    break;
                case ACTION_BAR:
                    p.sendMessage(ChatMessageType.ACTION_BAR, parseToComponent(str));
                    break;
            }
        }
    }

    public static String parseToString(String str) {
        return ChatColor.translateAlternateColorCodes('&', SharedChatUtils.parseText(str));
    }

    public static BaseComponent[] parseToComponent(String str) {
        return TextComponent.fromLegacyText(parseToString(str));
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
}
