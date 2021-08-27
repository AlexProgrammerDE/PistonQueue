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
import net.pistonmaster.pistonqueue.shared.*;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;

import java.util.Map;
import java.util.UUID;

public class QueueListenerVelocity extends QueueListenerShared {
    private final PistonQueueVelocity plugin;

    public QueueListenerVelocity(PistonQueueVelocity plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        PlayerWrapper player = plugin.wrapPlayer(event.getPlayer());

        if (StorageTool.isShadowBanned(player.getUniqueId()) && Config.SHADOWBANTYPE == BanType.KICK) {
            player.disconnect(Config.SERVERDOWNKICKMESSAGE);
        }
    }

    @Subscribe
    public void onSend(ServerPreConnectEvent event) {
        PlayerWrapper player = plugin.wrapPlayer(event.getPlayer());

        if (Config.AUTHFIRST) {
            if (Config.ALWAYSQUEUE)
                return;

            if (isAnyoneQueuedOfType(player))
                return;

            if (!isPlayersQueueFull(player) && event.getResult().getServer().get().getServerInfo().getName().equals(Config.QUEUESERVER))
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getProxyServer().getServer(Config.MAINSERVER).get()));
        } else {
            if (!event.getPlayer().getCurrentServer().isPresent()) {
                if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if auth is not enabled
                    if (Config.ALWAYSQUEUE || isServerFull(player) || (!mainOnline && !Config.KICKWHENDOWN)) {
                        if (player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                            event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getProxyServer().getServer(Config.MAINSERVER).get()));
                        } else {
                            putQueue(player, event);
                        }
                    }
                } else {
                    player.disconnect(Config.SERVERDOWNKICKMESSAGE);
                }
            }
        }
    }

    @Subscribe
    public void onQueueSend(ServerConnectedEvent event) {
        PlayerWrapper player = plugin.wrapPlayer(event.getPlayer());

        if (Config.AUTHFIRST) {
            if (isAuthToQueue(event) && player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                player.connect(Config.MAINSERVER);
                return;
            }

            // Its null when joining!
            if (!event.getPreviousServer().isPresent() && event.getServer().getServerInfo().getName().equals(Config.QUEUESERVER)) {
                if (Config.ALLOWAUTHSKIP)
                    putQueueAuthFirst(player);
            } else if (isAuthToQueue(event)) {
                putQueueAuthFirst(player);
            }
        }
    }

    private void putQueue(PlayerWrapper player, ServerPreConnectEvent event) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Redirect the player to the queue.
        String originalTarget = event.getResult().getServer().get().getServerInfo().getName();

        event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getProxyServer().getServer(Config.QUEUESERVER).get()));

        Map<UUID, String> queueMap = type.getQueueMap();

        // Store the data concerning the player's original destination
        if (Config.FORCEMAINSERVER) {
            queueMap.put(player.getUniqueId(), Config.MAINSERVER);
        } else {
            queueMap.put(player.getUniqueId(), originalTarget);
        }
    }

    private boolean isAuthToQueue(ServerConnectedEvent event) {
        return event.getPreviousServer().isPresent() && event.getPreviousServer().get().getServerInfo().getName().equals(Config.AUTHSERVER) && event.getServer().getServerInfo().getName().equals(Config.QUEUESERVER);
    }
}
