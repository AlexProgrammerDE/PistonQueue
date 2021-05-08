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
package net.pistonmaster.pistonqueue.bungee.hooks;

import net.pistonmaster.pistonmotd.api.PlaceholderParser;
import net.pistonmaster.pistonmotd.api.PlaceholderUtil;
import net.pistonmaster.pistonqueue.bungee.QueueAPI;

public class PistonMOTDPlaceholder implements PlaceholderParser {
    public PistonMOTDPlaceholder() {
        PlaceholderUtil.registerParser(this);
    }

    @Override
    public String parseString(String s) {
        return s.replace("%pistonqueue_regular%", String.valueOf(QueueAPI.getRegularSize()))
                .replace("%pistonqueue_priority%", String.valueOf(QueueAPI.getPrioritySize()))
                .replace("%pistonqueue_veteran%", String.valueOf(QueueAPI.getVeteranSize()));
    }
}
