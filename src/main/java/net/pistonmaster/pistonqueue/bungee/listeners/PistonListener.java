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
package net.pistonmaster.pistonqueue.bungee.listeners;

import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.shared.Config;

public final class PistonListener implements Listener {
    @EventHandler
    public void onPreLogin(PreLoginEvent ple) {
        if (ple.isCancelled())
            return;

        if (Config.ENABLE_REGEX && !ple.getConnection().getName().matches(Config.REGEX)) {
            ple.setCancelReason(ChatUtils.parseToComponent(Config.REGEX_MESSAGE.replace("%regex%", Config.REGEX)));
            ple.setCancelled(true);
        }
    }
}
