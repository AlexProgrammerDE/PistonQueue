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

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueue;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.utils.Config;

import java.util.ArrayList;
import java.util.List;

public final class PistonListener implements Listener {
    private final PistonQueue plugin;

    public PistonListener(PistonQueue plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent ple) {
        if (ple.isCancelled())
            return;

        if (!ple.getConnection().getName().matches(Config.REGEX)) {
            ple.setCancelReason(ChatUtils.parseToComponent(Config.REGEXMESSAGE.replace("%regex%", Config.REGEX)));
            ple.setCancelled(true);
        }
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        if (Config.IFMAINDOWNSENDTOQUEUE && event.getKickedFrom() == plugin.getProxy().getServerInfo(Config.MAINSERVER)) {
            for (String str : Config.DOWNWORDLIST) {
                if (TextComponent.toLegacyText(event.getKickReasonComponent()).toLowerCase().contains(str)) {
                    event.setCancelServer(plugin.getProxy().getServerInfo(Config.QUEUESERVER));
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(ChatMessageType.CHAT, ChatUtils.parseToComponent(Config.IFMAINDOWNSENDTOQUEUEMESSAGE));
                    // plugin.getQueueListener().putQueueAuthFirst(event.getPlayer());
                    plugin.getQueueListener().getNoRecoveryMessage().add(event.getPlayer().getUniqueId());
                    break;
                }
            }
        }

        if (Config.ENABLEKICKMESSAGE) {
            event.setKickReasonComponent(ChatUtils.parseToComponent(Config.KICKMESSAGE));
        }
    }
}
