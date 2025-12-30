package net.pistonmaster.pistonqueue.shared.queue.logic;

import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.ServerStatusManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerStatusManagerTest {

  @Test
  void onlyOnlineAfterConfiguredCount() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setMinOnlineChecks(3);

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    ServerStatusManager serverStatusManager = new ServerStatusManager(plugin::getConfiguration);

    //first time, should not be online
    serverStatusManager.online("testServer");
    assertTrue(serverStatusManager.getOnlineServers().isEmpty());
    assertEquals(1, serverStatusManager.getOnlinePingCount("testServer"));

    //second time, should not be online
    serverStatusManager.online("testServer");
    assertTrue(serverStatusManager.getOnlineServers().isEmpty());
    assertEquals(2, serverStatusManager.getOnlinePingCount("testServer"));

    //third time, should be online
    serverStatusManager.online("testServer");
    assertTrue(serverStatusManager.getOnlineServers().contains("testServer"));
    assertEquals(3, serverStatusManager.getOnlinePingCount("testServer"));

    //fourth time, should still be online, but count should not be over our limit (3)
    serverStatusManager.online("testServer");
    assertTrue(serverStatusManager.getOnlineServers().contains("testServer"));
    assertEquals(3, serverStatusManager.getOnlinePingCount("testServer"));
  }

  @Test
  void offlineAfterOneTime() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setMinOnlineChecks(3);

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    ServerStatusManager serverStatusManager = new ServerStatusManager(plugin::getConfiguration);

    //call online three times to make it online
    serverStatusManager.online("testServer");
    serverStatusManager.online("testServer");
    serverStatusManager.online("testServer");
    assertTrue(serverStatusManager.getOnlineServers().contains("testServer"));

    //now call offline once, should be offline now
    serverStatusManager.offline("testServer");
    assertTrue(serverStatusManager.getOnlineServers().isEmpty());
  }

  @Test
  void onlineWithZeroMinChecks() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setMinOnlineChecks(0);

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    ServerStatusManager serverStatusManager = new ServerStatusManager(plugin::getConfiguration);

    //call online once to make it online
    serverStatusManager.online("testServer");
    assertTrue(serverStatusManager.getOnlineServers().contains("testServer"));
  }

  @Test
  void onlineWithZeroMinChecksOverflow() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setMinOnlineChecks(0);

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    ServerStatusManager serverStatusManager = new ServerStatusManager(plugin::getConfiguration);

    //call online 10 times, should still be online and count should be 1
    for (int i = 0; i < 10; i++) {
      serverStatusManager.online("testServer");
    }
    assertTrue(serverStatusManager.getOnlineServers().contains("testServer"));
    assertEquals(1, serverStatusManager.getOnlinePingCount("testServer"));
  }

  @Test
  void onlineWithNegativeMinChecks() {
    Config config = QueueTestUtils.createConfigWithSingleQueueType(5);
    config.setMinOnlineChecks(-1);

    QueueTestUtils.TestQueuePlugin plugin = new QueueTestUtils.TestQueuePlugin(config);
    ServerStatusManager serverStatusManager = new ServerStatusManager(plugin::getConfiguration);

    //call online 10 times, should still be online and count should be 1
    for (int i = 0; i < 10; i++) {
      serverStatusManager.online("testServer");
    }
    assertTrue(serverStatusManager.getOnlineServers().contains("testServer"));
    assertEquals(1, serverStatusManager.getOnlinePingCount("testServer"));
  }
}
