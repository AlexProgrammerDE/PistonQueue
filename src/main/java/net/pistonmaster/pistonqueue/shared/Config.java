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
package net.pistonmaster.pistonqueue.shared;

import net.pistonmaster.pistonqueue.shared.utils.BanType;

import java.util.List;

/**
 * Config
 */
public final class Config {
    public static String SERVER_NAME, SERVER_IS_FULL_MESSAGE, QUEUE_POSITION, JOINING_MAIN_SERVER,
            QUEUE_BYPASS_PERMISSION, QUEUE_SERVER, QUEUE_PRIORITY_PERMISSION, REGEX,
            KICK_MESSAGE, ADMIN_PERMISSION, MAIN_SERVER, AUTH_SERVER, SERVER_DOWN_KICK_MESSAGE,
            QUEUE_VETERAN_PERMISSION, REGEX_MESSAGE, PAUSE_QUEUE_IF_MAIN_DOWN_MESSAGE,
            SHADOW_BAN_MESSAGE, IF_MAIN_DOWN_SEND_TO_QUEUE_MESSAGE, RECOVERY_MESSAGE;

    public static boolean POSITION_MESSAGE_HOT_BAR, ENABLE_KICK_MESSAGE,
            ENABLE_AUTH_SERVER, ALWAYS_QUEUE, REGISTER_TAB, AUTH_FIRST, POSITION_MESSAGE_CHAT,
            PAUSE_QUEUE_IF_MAIN_DOWN, KICK_WHEN_DOWN, FORCE_MAIN_SERVER, ALLOW_AUTH_SKIP,
            IF_MAIN_DOWN_SEND_TO_QUEUE, RECOVERY, ENABLE_REGEX, SEND_XP_SOUND;

    public static int QUEUE_MOVE_DELAY, SERVER_ONLINE_CHECK_DELAY, POSITION_MESSAGE_DELAY,
            START_TIME, REGULAR_SLOTS, PRIORITY_SLOTS, VETERAN_SLOTS, CUSTOM_PERCENT_PERCENTAGE,
            MAX_PLAYERS_PER_MOVE;

    public static List<String> HEADER, FOOTER, HEADER_PRIORITY, FOOTER_PRIORITY,
            HEADER_VETERAN, FOOTER_VETERAN, DOWN_WORD_LIST;

    public static BanType SHADOW_BAN_TYPE;

    private Config() {
    }
}
