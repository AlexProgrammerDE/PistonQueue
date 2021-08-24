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
package net.pistonmaster.pistonqueue.velocity.utils;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.pistonmaster.pistonqueue.utils.QueueAPI;

import java.time.Duration;
import java.util.List;

public class ChatUtils {
    private ChatUtils() {
    }

    public static TextComponent parseToComponent(String str) {
        return LegacyComponentSerializer.legacySection().deserialize(parseToString(str));
    }

    public static String parseToString(String str) {
        return LegacyComponentSerializer.legacySection().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(parseText(str)));
    }

    private static String parseText(String text) {
        text = text.replace("%veteran%", String.valueOf(QueueAPI.getVeteranSize()));
        text = text.replace("%priority%", String.valueOf(QueueAPI.getPrioritySize()));
        text = text.replace("%regular%", String.valueOf(QueueAPI.getRegularSize()));
        text = text.replace("%position%", "None");
        text = text.replace("%wait%", "None");

        return text;
    }

    public static TextComponent parseTab(List<String> tab) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < tab.size(); i++) {
            builder.append(parseToString(tab.get(i)));

            if (i != (tab.size() - 1)) {
                builder.append("\n");
            }
        }

        return LegacyComponentSerializer.legacySection().deserialize(builder.toString());
    }

    public static String formatDuration(String str, Duration duration, int position) {
        String format = String.format("%dh %dm", duration.toHours(), duration.toMinutes() % 60);

        if (duration.toHours() == 0)
            format = String.format("%dm", duration.toMinutes() == 0 ? 1 : duration.toMinutes());

        return str.replace("%position%", String.valueOf(position)).replace("%wait%", format);
    }
}
