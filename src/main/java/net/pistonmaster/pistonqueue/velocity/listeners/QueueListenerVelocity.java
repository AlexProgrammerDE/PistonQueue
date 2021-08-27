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
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.pistonmaster.pistonqueue.shared.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.QueueListenerShared;
import net.pistonmaster.pistonqueue.shared.events.PQServerConnectedEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;

import java.util.Optional;

public class QueueListenerVelocity extends QueueListenerShared {
    private final PistonQueueVelocity plugin;

    public QueueListenerVelocity(PistonQueueVelocity plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        onPostLogin(plugin.wrapPlayer(event.getPlayer()));
    }

    @Subscribe
    public void onSend(ServerPreConnectEvent event) {
        onPreConnect(wrap(event));
    }

    @Subscribe
    public void onQueueSend(ServerConnectedEvent event) {
        onConnected(wrap(event));
    }

    private PQServerConnectedEvent wrap(ServerConnectedEvent event) {
        return new PQServerConnectedEvent() {
            @Override
            public PlayerWrapper getPlayer() {
                return plugin.wrapPlayer(event.getPlayer());
            }

            @Override
            public Optional<String> getPreviousServer() {
                return event.getPreviousServer().map(RegisteredServer::getServerInfo).map(ServerInfo::getName);
            }

            @Override
            public String getServer() {
                return event.getServer().getServerInfo().getName();
            }
        };
    }

    private PQServerPreConnectEvent wrap(ServerPreConnectEvent event) {
        return new PQServerPreConnectEvent() {
            @Override
            public PlayerWrapper getPlayer() {
                return plugin.wrapPlayer(event.getPlayer());
            }

            @Override
            public String getTarget() {
                return event.getResult().getServer().get().getServerInfo().getName();
            }

            @Override
            public void setTarget(String server) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getProxyServer().getServer(server).get()));
            }
        };
    }
}
