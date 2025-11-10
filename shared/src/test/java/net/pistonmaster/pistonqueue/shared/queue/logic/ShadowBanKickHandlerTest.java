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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ShadowBanKickHandlerTest {

  @Test
  void kicksShadowBannedPlayerWhenKickTypeEnabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.SHADOW_BAN_TYPE = BanType.KICK;
    config.SERVER_DOWN_KICK_MESSAGE = "You are shadow banned";

    ShadowBanKickHandler handler = new ShadowBanKickHandler(config);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("BannedPlayer");

    // Simulate shadow ban
    QueueTestUtils.ensureStorageToolInitialized();
    StorageTool.shadowBanPlayer("BannedPlayer", Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + 86400000))); // 1 day from now

    handler.handleShadowBanKick(player);

    assertTrue(player.isDisconnected());
    assertEquals("You are shadow banned", player.getDisconnectMessage());
  }

  @Test
  void doesNotKickNonShadowBannedPlayer() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.SHADOW_BAN_TYPE = BanType.KICK;

    ShadowBanKickHandler handler = new ShadowBanKickHandler(config);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("GoodPlayer");

    handler.handleShadowBanKick(player);

    assertFalse(player.isDisconnected());
  }

  @Test
  void doesNotKickShadowBannedPlayerWhenKickTypeDisabled() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.SHADOW_BAN_TYPE = BanType.LOOP; // Not KICK type

    ShadowBanKickHandler handler = new ShadowBanKickHandler(config);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("BannedPlayer");

    // Simulate shadow ban
    QueueTestUtils.ensureStorageToolInitialized();
    StorageTool.shadowBanPlayer("BannedPlayer", Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + 86400000))); // 1 day from now

    handler.handleShadowBanKick(player);

    assertFalse(player.isDisconnected());
  }

  @Test
  void usesCorrectKickMessage() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.SHADOW_BAN_TYPE = BanType.KICK;
    config.SERVER_DOWN_KICK_MESSAGE = "Custom shadow ban message";

    ShadowBanKickHandler handler = new ShadowBanKickHandler(config);
    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    QueueTestUtils.TestPlayer player = plugin.registerPlayer("BannedPlayer");

    // Simulate shadow ban
    QueueTestUtils.ensureStorageToolInitialized();
    StorageTool.shadowBanPlayer("BannedPlayer", Date.from(Instant.ofEpochMilli(System.currentTimeMillis() + 86400000))); // 1 day from now

    handler.handleShadowBanKick(player);

    assertTrue(player.isDisconnected());
    assertEquals("Custom shadow ban message", player.getDisconnectMessage());
  }
}
