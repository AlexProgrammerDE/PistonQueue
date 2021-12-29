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

import lombok.Getter;
import net.pistonmaster.pistonqueue.shared.utils.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public enum QueueType {
    REGULAR,
    PRIORITY,
    VETERAN;

    @Getter
    private final Map<UUID, String> queueMap = Collections.synchronizedMap(new LinkedHashMap<>());

    @Getter
    private final Map<Integer, Duration> durationToPosition = Collections.synchronizedMap(new LinkedHashMap<>());

    @Getter
    private final Map<UUID, List<Pair<Integer, Instant>>> positionCache = new ConcurrentHashMap<>();

    @Getter
    private final AtomicInteger playersWithTypeInMain = new AtomicInteger();

    public static QueueType getQueueType(Function<String, Boolean> player) {
        if (player.apply(Config.QUEUE_VETERAN_PERMISSION)) {
            return VETERAN;
        } else if (player.apply(Config.QUEUE_PRIORITY_PERMISSION)) {
            return PRIORITY;
        } else {
            return REGULAR;
        }
    }

    public List<String> getHeader() {
        switch (this) {
            case VETERAN:
                return Config.HEADER_VETERAN;
            case PRIORITY:
                return Config.HEADER_PRIORITY;
            default:
                return Config.HEADER;
        }
    }

    public List<String> getFooter() {
        switch (this) {
            case VETERAN:
                return Config.FOOTER_VETERAN;
            case PRIORITY:
                return Config.FOOTER_PRIORITY;
            default:
                return Config.FOOTER;
        }
    }

    public int getReservedSlots() {
        switch (this) {
            case VETERAN:
                return Config.VETERAN_SLOTS;
            case PRIORITY:
                return Config.PRIORITY_SLOTS;
            default:
                return Config.REGULAR_SLOTS;
        }
    }
}
