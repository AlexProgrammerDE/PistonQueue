package net.pistonmaster.pistonqueue.shared.queue.logic;

import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.queue.QueueListenerShared;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class QueueListenerConstructionTest {
  @Test
  void statusChecksContinueToUseReloadedConfiguration() {
    var config = QueueTestUtils.createConfigWithSingleQueueType(10);
    config.setMinOnlineChecks(1);
    var plugin = new QueueTestUtils.TestQueuePlugin(config);
    var listener = new QueueListenerShared(plugin) {};
    var status = listener.getServerStatusManager();
    status.online("target");
    assertTrue(status.getOnlineServers().contains("target"));
    config.setMinOnlineChecks(2);
    assertFalse(status.getOnlineServers().contains("target"));
    status.online("target");
    assertTrue(status.getOnlineServers().contains("target"));
  }

  @Test
  void rejectsMissingConfigurationDuringConstruction() {
    var plugin = mock(PistonQueuePlugin.class);
    assertThrows(NullPointerException.class, () -> new QueueListenerShared(plugin) {});
  }
}
