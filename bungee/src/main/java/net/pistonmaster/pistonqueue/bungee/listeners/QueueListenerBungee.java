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
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueueBungee;
import net.pistonmaster.pistonqueue.bungee.utils.ChatUtils;
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;
import net.pistonmaster.pistonqueue.shared.events.PQPreLoginEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.queue.QueueListenerShared;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.Optional;

public final class QueueListenerBungee extends QueueListenerShared implements Listener {
    private final PistonQueueBungee plugin;

    public QueueListenerBungee(PistonQueueBungee plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        onPreLogin(wrap(event));
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        onPostLogin(plugin.wrapPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onSend(ServerConnectEvent event) {
        onPreConnect(wrap(event));
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        onKick(wrap(event));
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
                event.setTarget(plugin.getProxy().getServerInfo(server));
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
            public void setKickMessage(String message) {
                event.setKickReasonComponent(ChatUtils.parseToComponent(message));
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

            @Override
            public boolean willDisconnect() {
                return !event.isCancelled();
            }
        };
    }

    private PQPreLoginEvent wrap(PreLoginEvent event) {
        return new PQPreLoginEvent() {
            @Override
            public boolean isCancelled() {
                return event.isCancelled();
            }

            @Override
            public void setCancelled(String reason) {
                event.setCancelReason(ChatUtils.parseToComponent(reason));
                event.setCancelled(true);
            }

            @Override
            public String getUsername() {
                return event.getConnection().getName();
            }
        };
    }
}
