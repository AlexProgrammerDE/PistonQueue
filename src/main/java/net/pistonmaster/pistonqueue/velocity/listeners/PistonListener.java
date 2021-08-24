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
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.pistonmaster.pistonqueue.utils.Config;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;
import net.pistonmaster.pistonqueue.velocity.utils.ChatUtils;

public class PistonListener {
    private final PistonQueueVelocity plugin;

    public PistonListener(PistonQueueVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent ple) {
        if (ple.getResult() != PreLoginEvent.PreLoginComponentResult.allowed())
            return;

        if (!ple.getUsername().matches(Config.REGEX)) {
            ple.setResult(PreLoginEvent.PreLoginComponentResult.denied(ChatUtils.parseToComponent(Config.REGEXMESSAGE.replace("%regex%", Config.REGEX))));
        }
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        if (Config.IFMAINDOWNSENDTOQUEUE && event.getServer() == plugin.getServer().getServer(Config.MAINSERVER).get()) {
            if (!event.getServerKickReason().isPresent())
                return;

            for (String str : Config.DOWNWORDLIST) {
                if (LegacyComponentSerializer.legacySection().serialize(event.getServerKickReason().get()).toLowerCase().contains(str)) {
                    event.setResult(KickedFromServerEvent.RedirectPlayer.create(plugin.getServer().getServer(Config.QUEUESERVER).get()));
                    event.getPlayer().sendMessage(ChatUtils.parseToComponent(Config.IFMAINDOWNSENDTOQUEUEMESSAGE));
                    // plugin.getQueueListener().putQueueAuthFirst(event.getPlayer());
                    plugin.getQueueListener().getNoRecoveryMessage().add(event.getPlayer().getUniqueId());
                    break;
                }
            }
        }

        if (Config.ENABLEKICKMESSAGE) {
            if (event.getResult() instanceof KickedFromServerEvent.DisconnectPlayer)
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(ChatUtils.parseToComponent(Config.KICKMESSAGE)));
        }
    }
}
