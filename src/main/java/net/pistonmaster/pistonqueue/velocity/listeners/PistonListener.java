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
package net.pistonmaster.pistonqueue.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.pistonmaster.pistonqueue.shared.Config;
import net.pistonmaster.pistonqueue.velocity.utils.ChatUtils;

public class PistonListener {
    @Subscribe
    public void onPreLogin(PreLoginEvent ple) {
        if (ple.getResult() != PreLoginEvent.PreLoginComponentResult.allowed())
            return;

        if (Config.ENABLE_REGEX && !ple.getUsername().matches(Config.REGEX)) {
            ple.setResult(PreLoginEvent.PreLoginComponentResult.denied(ChatUtils.parseToComponent(Config.REGEX_MESSAGE.replace("%regex%", Config.REGEX))));
        }
    }
}
