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
package net.pistonmaster.pistonqueue.shared.queue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.pistonqueue.shared.config.Config;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class QueueType {
    private final Map<UUID, String> queueMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<Integer, Duration> durationFromPosition = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<UUID, Map<Integer, Instant>> positionCache = new ConcurrentHashMap<>();
    private final AtomicInteger playersWithTypeInTarget = new AtomicInteger();
    private final String name;
    @Setter
    private int order;
    @Setter
    private String permission;
    @Setter
    private int reservedSlots;
    @Setter
    private List<String> header;
    @Setter
    private List<String> footer;

    public static QueueType getQueueType(Predicate<String> player) {
        for (QueueType type : Config.QUEUE_TYPES) {
            if (type.getPermission().equals("default") || player.test(type.getPermission())) {
                return type;
            }
        }
        throw new RuntimeException("No queue type found for player! (There is no default queue type)");
    }
}
