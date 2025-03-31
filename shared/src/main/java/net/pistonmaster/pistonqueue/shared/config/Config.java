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
package net.pistonmaster.pistonqueue.shared.config;

import net.pistonmaster.pistonqueue.shared.queue.BanType;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;

import java.util.List;

public final class Config {
    public static String SERVER_NAME, SERVER_IS_FULL_MESSAGE, QUEUE_POSITION, JOINING_TARGET_SERVER,
            QUEUE_BYPASS_PERMISSION, QUEUE_SERVER, USERNAME_REGEX,
            KICK_MESSAGE, ADMIN_PERMISSION, TARGET_SERVER, SOURCE_SERVER, SERVER_DOWN_KICK_MESSAGE,
            USERNAME_REGEX_MESSAGE, PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE,
            SHADOW_BAN_MESSAGE, IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE, RECOVERY_MESSAGE;

    public static boolean POSITION_PLAYER_LIST, ENABLE_KICK_MESSAGE,
            ENABLE_SOURCE_SERVER, ALWAYS_QUEUE, REGISTER_TAB,
            POSITION_MESSAGE_CHAT, POSITION_MESSAGE_HOT_BAR,
            PAUSE_QUEUE_IF_TARGET_DOWN, KICK_WHEN_DOWN, FORCE_TARGET_SERVER,
            IF_TARGET_DOWN_SEND_TO_QUEUE, RECOVERY, ENABLE_USERNAME_REGEX, SEND_XP_SOUND;

    public static int QUEUE_MOVE_DELAY, SERVER_ONLINE_CHECK_DELAY, POSITION_MESSAGE_DELAY,
            PERCENT, MAX_PLAYERS_PER_MOVE;

    public static List<String> DOWN_WORD_LIST, KICK_WHEN_DOWN_SERVERS;
    public static QueueType[] QUEUE_TYPES; // Not allowed to be resized due to data corruption
    public static BanType SHADOW_BAN_TYPE;

    private Config() {
    }
}
