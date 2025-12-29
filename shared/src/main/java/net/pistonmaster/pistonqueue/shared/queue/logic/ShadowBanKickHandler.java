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
package net.pistonmaster.pistonqueue.shared.queue.logic;

import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.BanType;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.Objects;

/// Handles shadow ban kicks for players after they log in.
public final class ShadowBanKickHandler {
  private final Config config;

  public ShadowBanKickHandler(Config config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  /// Checks if the player should be kicked due to shadow ban and disconnects them if so.
  ///
  /// @param player the player who just logged in
  public void handleShadowBanKick(PlayerWrapper player) {
    if (StorageTool.isShadowBanned(player.getName()) && config.shadowBanType() == BanType.KICK) {
      player.disconnect(config.serverDownKickMessage());
    }
  }
}
