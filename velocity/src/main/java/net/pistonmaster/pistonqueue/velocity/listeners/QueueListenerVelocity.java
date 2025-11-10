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
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.pistonmaster.pistonqueue.shared.events.PQKickedFromServerEvent;
import net.pistonmaster.pistonqueue.shared.events.PQPreLoginEvent;
import net.pistonmaster.pistonqueue.shared.events.PQServerPreConnectEvent;
import net.pistonmaster.pistonqueue.shared.queue.QueueListenerShared;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;
import net.pistonmaster.pistonqueue.velocity.utils.ChatUtils;

import java.util.Optional;

public final class QueueListenerVelocity extends QueueListenerShared {
  private final PistonQueueVelocity plugin;

  public QueueListenerVelocity(PistonQueueVelocity plugin) {
    super(plugin);
    this.plugin = plugin;
  }

  @Subscribe
  public void onPreLogin(PreLoginEvent event) {
    onPreLogin(wrap(event));
  }

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    onPostLogin(plugin.wrapPlayer(event.getPlayer()));
  }

  @Subscribe
  public void onKick(KickedFromServerEvent event) {
    onKick(wrap(event));
  }

  @Subscribe
  public void onSend(ServerPreConnectEvent event) {
    onPreConnect(wrap(event));
  }

  private PQServerPreConnectEvent wrap(ServerPreConnectEvent event) {
    return new PQServerPreConnectEvent() {
      @Override
      public PlayerWrapper getPlayer() {
        return plugin.wrapPlayer(event.getPlayer());
      }

      @Override
      public Optional<String> getTarget() {
        return event.getResult().getServer().map(RegisteredServer::getServerInfo).map(ServerInfo::getName);
      }

      @Override
      public void setTarget(String server) {
        event.setResult(ServerPreConnectEvent.ServerResult.allowed(plugin.getProxyServer().getServer(server).orElseThrow(() ->
          new IllegalArgumentException("Server %s not found".formatted(server)))));
      }
    };
  }

  private PQKickedFromServerEvent wrap(KickedFromServerEvent event) {
    return new PQKickedFromServerEvent() {
      @Override
      public void setCancelServer(String server) {
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(plugin.getProxyServer().getServer(server).orElseThrow(() ->
          new IllegalArgumentException("Server %s not found".formatted(server)))));
      }

      @Override
      public void setKickMessage(String message) {
        event.setResult(KickedFromServerEvent.DisconnectPlayer.create(ChatUtils.parseToComponent(plugin.getConfiguration(), message)));
      }

      @Override
      public PlayerWrapper getPlayer() {
        return plugin.wrapPlayer(event.getPlayer());
      }

      @Override
      public String getKickedFrom() {
        return event.getServer().getServerInfo().getName();
      }

      @Override
      public Optional<String> getKickReason() {
        return event.getServerKickReason().map(LegacyComponentSerializer.legacySection()::serialize);
      }

      @Override
      public boolean willDisconnect() {
        return event.getResult().isAllowed();
      }
    };
  }

  private PQPreLoginEvent wrap(PreLoginEvent event) {
    return new PQPreLoginEvent() {
      @Override
      public boolean isCancelled() {
        return event.getResult() != PreLoginEvent.PreLoginComponentResult.allowed();
      }

      @Override
      public void setCancelled(String reason) {
        event.setResult(PreLoginEvent.PreLoginComponentResult.denied(ChatUtils.parseToComponent(plugin.getConfiguration(), reason)));
      }

      @Override
      public String getUsername() {
        return event.getUsername();
      }
    };
  }
}
