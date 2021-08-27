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

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.pistonmaster.pistonqueue.bungee.PistonQueueBungee;
import net.pistonmaster.pistonqueue.shared.Config;
import net.pistonmaster.pistonqueue.shared.PlayerWrapper;
import net.pistonmaster.pistonqueue.shared.QueueListenerShared;
import net.pistonmaster.pistonqueue.shared.QueueType;

import java.util.Map;
import java.util.UUID;

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
    public void onSend(ServerConnectEvent event) {
        PlayerWrapper player = plugin.wrapPlayer(event.getPlayer());

        if (Config.AUTHFIRST) {
            if (Config.ALWAYSQUEUE)
                return;

            if (isAnyoneQueuedOfType(player))
                return;

            if (!isPlayersQueueFull(player) && event.getTarget().getName().equals(Config.QUEUESERVER))
                event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
        } else {
            if (!player.getCurrentServer().isPresent()) {
                if (!Config.KICKWHENDOWN || (mainOnline && queueOnline && authOnline)) { // authOnline is always true if auth is not enabled
                    if (Config.ALWAYSQUEUE || isServerFull(player) || (!mainOnline && !Config.KICKWHENDOWN)) {
                        if (player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                            event.setTarget(plugin.getProxy().getServerInfo(Config.MAINSERVER));
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

    @EventHandler
    public void onQueueSend(ServerSwitchEvent event) {
        PlayerWrapper player = plugin.wrapPlayer(event.getPlayer());

        if (Config.AUTHFIRST) {
            if (isAuthToQueue(event) && player.hasPermission(Config.QUEUEBYPASSPERMISSION)) {
                player.connect(Config.MAINSERVER);
                return;
            }

            // Its null when joining!
            if (event.getFrom() == null && player.getCurrentServer().isPresent() && player.getCurrentServer().get().equals(Config.QUEUESERVER)) {
                if (Config.ALLOWAUTHSKIP)
                    putQueueAuthFirst(player);
            } else if (isAuthToQueue(event)) {
                putQueueAuthFirst(player);
            }
        }
    }


    private void putQueue(PlayerWrapper player, ServerConnectEvent event) {
        QueueType type = QueueType.getQueueType(player::hasPermission);

        preQueueAdding(player, type.getHeader(), type.getFooter());

        // Redirect the player to the queue.
        String originalTarget = event.getTarget().getName();

        event.setTarget(plugin.getProxy().getServerInfo(Config.QUEUESERVER));

        Map<UUID, String> queueMap = type.getQueueMap();

        // Store the data concerning the player's original destination
        if (Config.FORCEMAINSERVER) {
            queueMap.put(player.getUniqueId(), Config.MAINSERVER);
        } else {
            queueMap.put(player.getUniqueId(), originalTarget);
        }
    }

    private boolean isAuthToQueue(ServerSwitchEvent event) {
        return event.getFrom() != null && event.getFrom().equals(plugin.getProxy().getServerInfo(Config.AUTHSERVER)) && event.getPlayer().getServer().getInfo().equals(plugin.getProxy().getServerInfo(Config.QUEUESERVER));
    }
}
