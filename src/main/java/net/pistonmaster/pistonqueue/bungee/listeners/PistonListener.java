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
import net.pistonmaster.pistonqueue.bungee.utils.Config;

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
                    plugin.getQueueListener().putQueueAuthFirst(event.getPlayer());
                    break;
                }
            }
        }

        if (Config.ENABLEKICKMESSAGE) {
            event.setKickReasonComponent(ChatUtils.parseToComponent(Config.KICKMESSAGE));
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing.Protocol protocol;
        ServerPing.Players players;

        if (Config.CUSTOMPROTOCOLENABLE) {
            ServerPing.Protocol provided = event.getResponse().getVersion();

            provided.setName(ChatUtils.parseToString(Config.CUSTOMPROTOCOL));

            protocol = provided;
        } else {
            protocol = event.getResponse().getVersion();
        }

        if (Config.SERVERPINGINFOENABLE) {
            List<ServerPing.PlayerInfo> info = new ArrayList<>();

            Config.SERVERPINGINFO.forEach(str -> info
                    .add(new ServerPing.PlayerInfo(
                            ChatUtils.parseToString(str),
                            String.valueOf(Config.SERVERPINGINFO.indexOf(str) - 1)
                    ))
            );

            players = new ServerPing.Players(Config.QUEUESERVERSLOTS, plugin.getProxy().getOnlineCount(), info.toArray(new ServerPing.PlayerInfo[0]));
        } else {
            players = event.getResponse().getPlayers();
        }

        ServerPing ping = new ServerPing(protocol, players, event.getResponse().getDescriptionComponent(), event.getResponse().getFaviconObject());
        event.setResponse(ping);
    }
}
