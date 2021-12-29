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

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueueBungee;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.shared.Config;
import net.pistonmaster.pistonqueue.shared.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.QueueListenerShared;
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerConnectedEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;

import java.util.Optional;

public final class QueueListenerBungee extends QueueListenerShared implements Listener {
    private final PistonQueueBungee plugin;

    public QueueListenerBungee(PistonQueueBungee plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        onPostLogin(plugin.wrapPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        onKick(wrap(event));

        if (Config.ENABLE_KICK_MESSAGE) {
            event.setKickReasonComponent(ChatUtils.parseToComponent(Config.KICK_MESSAGE));
        }
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        onPreConnect(wrap(event));
    }

    @EventHandler
    public void onQueueSend(ServerSwitchEvent event) {
        onConnected(wrap(event));
    }

    private PQServerConnectedEvent wrap(ServerSwitchEvent event) {
        return new PQServerConnectedEvent() {
            @Override
            public PlayerWrapper getPlayer() {
                return plugin.wrapPlayer(event.getPlayer());
            }

            @Override
            public Optional<String> getPreviousServer() {
                return Optional.ofNullable(event.getFrom()).map(ServerInfo::getName);
            }

            @Override
            public String getServer() {
                return event.getPlayer().getServer().getInfo().getName();
            }
        };
    }

    private PQServerPreConnectEvent wrap(ServerConnectEvent event) {
        return new PQServerPreConnectEvent() {
            @Override
            public PlayerWrapper getPlayer() {
                return plugin.wrapPlayer(event.getPlayer());
            }

            @Override
            public Optional<String> getTarget() {
                return Optional.of(event.getTarget().getName());
            }

            @Override
            public void setTarget(String server) {
                event.setTarget(plugin.getProxy().getServerInfo(Config.QUEUE_SERVER));
            }
        };
    }

    private PQKickedFromServerEvent wrap(ServerKickEvent event) {
        return new PQKickedFromServerEvent() {
            @Override
            public void setCancelServer(String server) {
                event.setCancelServer(plugin.getProxy().getServerInfo(server));
                event.setCancelled(true);
            }

            @Override
            public PlayerWrapper getPlayer() {
                return plugin.wrapPlayer(event.getPlayer());
            }

            @Override
            public String getKickedFrom() {
                return event.getKickedFrom().getName();
            }

            @Override
            public Optional<String> getKickReason() {
                return Optional.ofNullable(event.getKickReasonComponent()).map(TextComponent::toLegacyText);
            }
        };
    }
}
