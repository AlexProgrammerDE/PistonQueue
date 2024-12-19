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
package net.pistonmaster.pistonqueue.shared.hooks;

import net.pistonmaster.pistonmotd.api.PlaceholderParser;
import net.pistonmaster.pistonmotd.api.PlaceholderUtil;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;

public final class PistonMOTDPlaceholder implements PlaceholderParser {
    public PistonMOTDPlaceholder() {
        PlaceholderUtil.registerParser(this);
    }

    @Override
    public String parseString(String s) {
        for (QueueType type : Config.QUEUE_TYPES) {
            s = s.replace("%pistonqueue_" + type.getName().toLowerCase() + "%", String.valueOf(type.getQueueMap().size()));
        }
        return s;
    }
}
