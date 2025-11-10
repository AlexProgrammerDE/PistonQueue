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
package net.pistonmaster.pistonqueue.bukkit;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public final class QueuePluginMessageListener implements PluginMessageListener {
  private final PistonQueueBukkit plugin;

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public void onPluginMessageReceived(@NotNull String channel, @NotNull Player messagePlayer, byte[] message) {
    if (!"piston:queue".equals(channel)) {
      return;
    }

    ByteArrayDataInput in = ByteStreams.newDataInput(message);
    String subChannel = in.readUTF();

    if (plugin.isPlayXP() && "xpV2".equals(subChannel)) {
      List<UUID> uuids = new ArrayList<>();
      int count = in.readInt();
      for (int i = 0; i < count; i++) {
        uuids.add(UUID.fromString(in.readUTF()));
      }

      for (UUID uuid : uuids) {
        Player target = plugin.getServer().getPlayer(uuid);

        if (target == null) {
          continue;
        }

        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100.0F, 1.0F);
      }
    }
  }
}
