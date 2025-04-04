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
import net.pistonmaster.pistonqueue.shared.utils.SharedChatUtils;

import java.util.List;
import java.util.stream.Collectors;

public final class ChatUtils {
    private ChatUtils() {
    }

    public static TextComponent parseToComponent(String str) {
        return LegacyComponentSerializer.legacySection().deserialize(parseToString(str));
    }

    public static String parseToString(String str) {
        return LegacyComponentSerializer.legacySection().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(SharedChatUtils.parseText(str)));
    }

    public static TextComponent parseTab(List<String> tab) {
        return parseToComponent(
            tab.stream()
                .map(ChatUtils::parseToString)
                .collect(Collectors.joining("\n"))
        );
    }
}
