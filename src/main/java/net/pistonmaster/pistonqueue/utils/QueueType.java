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
package net.pistonmaster.pistonqueue.utils;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public enum QueueType {
    REGULAR,
    PRIORITY,
    VETERAN;

    @Getter
    @Setter
    private Map<UUID, String> queueMap = new LinkedHashMap<>();

    @Getter
    private final Map<Integer, Duration> durationToPosition = new LinkedHashMap<>();

    @Getter
    private final Map<UUID, List<Pair<Integer, Instant>>> positionCache = new HashMap<>();

    @Setter
    @Getter
    private int playersWithTypeInMain = 0;

    public static QueueType getQueueType(Function<String, Boolean> player) {
        if (player.apply(Config.QUEUEVETERANPERMISSION)) {
            return VETERAN;
        } else if (player.apply(Config.QUEUEPRIORITYPERMISSION)) {
            return PRIORITY;
        } else {
            return REGULAR;
        }
    }

    public List<String> getHeader() {
        switch (this) {
            case VETERAN:
                return Config.HEADERVETERAN;
            case PRIORITY:
                return Config.HEADERPRIORITY;
            default:
                return Config.HEADER;
        }
    }

    public List<String> getFooter() {
        switch (this) {
            case VETERAN:
                return Config.FOOTERVETERAN;
            case PRIORITY:
                return Config.FOOTERPRIORITY;
            default:
                return Config.FOOTER;
        }
    }

    public int getReservatedSlots() {
        switch (this) {
            case VETERAN:
                return Config.VETERANSLOTS;
            case PRIORITY:
                return Config.PRIORITYSLOTS;
            default:
                return Config.REGULARSLOTS;
        }
    }


}
