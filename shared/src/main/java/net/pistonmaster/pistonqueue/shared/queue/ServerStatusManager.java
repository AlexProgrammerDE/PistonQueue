package net.pistonmaster.pistonqueue.shared.queue;

import net.pistonmaster.pistonqueue.shared.config.Config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServerStatusManager {
  private final Supplier<Config> configSupplier;

  private final Map<String, Integer> onlinePingCounts = new ConcurrentHashMap<>();

  public ServerStatusManager(Supplier<Config> configSupplier) {
    this.configSupplier = configSupplier;
  }

  public void online(String server) {
    onlinePingCounts.merge(server, 1, (oldValue, newValue) -> {
      //this prevents overflows, count never goes over the configured amount
      final int sum = Integer.sum(oldValue, newValue);
      if (sum > configSupplier.get().minOnlineChecks()) {
        return oldValue;
      }
      return sum;
    });
  }

  public void offline(String server) {
    onlinePingCounts.remove(server);
  }

  public Set<String> getOnlineServers() {
    return onlinePingCounts.entrySet().stream()
      .filter(entry -> entry.getValue() >= configSupplier.get().minOnlineChecks())
      .map(Map.Entry::getKey)
      .collect(Collectors.toSet());
  }

  // For testing purposes so we can assert there is no overflow risk
  public int getOnlinePingCount(String server) {
    return onlinePingCounts.getOrDefault(server, 0);
  }
}
