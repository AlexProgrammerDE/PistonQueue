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

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class QueueType {
    public static final QueueType REGULAR = new QueueType();
    public static final QueueType PRIORITY = new QueueType();
    public static final QueueType VETERAN = new QueueType();
    @Getter
    private final Map<UUID, String> queueMap = Collections.synchronizedMap(new LinkedHashMap<>());
    @Getter
    private final Map<Integer, Duration> durationToPosition = Collections.synchronizedMap(new LinkedHashMap<>());
    @Getter
    private final Map<UUID, Map<Integer, Instant>> positionCache = new ConcurrentHashMap<>();
    @Getter
    private final AtomicInteger playersWithTypeInMain = new AtomicInteger();

    public static QueueType[] values() {
        return new QueueType[]{REGULAR, PRIORITY, VETERAN};
    }

    public static QueueType getQueueType(Predicate<String> player) {
        if (player.test(Config.QUEUE_VETERAN_PERMISSION)) {
            return VETERAN;
        } else if (player.test(Config.QUEUE_PRIORITY_PERMISSION)) {
            return PRIORITY;
        } else {
            return REGULAR;
        }
    }

    public List<String> getHeader() {
        if (VETERAN.equals(this)) {
            return Config.HEADER_VETERAN;
        } else if (PRIORITY.equals(this)) {
            return Config.HEADER_PRIORITY;
        }
        return Config.HEADER;
    }

    public List<String> getFooter() {
        if (VETERAN.equals(this)) {
            return Config.FOOTER_VETERAN;
        } else if (PRIORITY.equals(this)) {
            return Config.FOOTER_PRIORITY;
        }
        return Config.FOOTER;
    }

    public int getReservedSlots() {
        if (VETERAN.equals(this)) {
            return Config.VETERAN_SLOTS;
        } else if (PRIORITY.equals(this)) {
            return Config.PRIORITY_SLOTS;
        }
        return Config.REGULAR_SLOTS;
    }
}
