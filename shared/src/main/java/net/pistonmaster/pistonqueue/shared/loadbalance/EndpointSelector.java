package net.pistonmaster.pistonqueue.shared.loadbalance;

import lombok.Getter;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.wrapper.ServerInfoWrapper;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class EndpointSelector {
  private EndpointSelector() {}

  @Getter
  private static final Map<String, LoadEntry> loadMap = new ConcurrentHashMap<>();

  public static Optional<EndpointConfig> select(PistonQueuePlugin plugin, LobbyGroupConfig group) {
    List<EndpointConfig> candidates = group.getEndpoints();
    if (candidates == null || candidates.isEmpty()) return Optional.empty();

    // Filter online
    List<EndpointConfig> online = candidates.stream()
      .filter(ep -> isOnline(plugin, group, ep))
      .collect(Collectors.toList());
    if (online.isEmpty()) return Optional.empty();

    // Min priority
    int minPrio = online.stream().mapToInt(EndpointConfig::getPriority).min().orElse(1);
    List<EndpointConfig> prio = online.stream().filter(ep -> ep.getPriority() == minPrio).collect(Collectors.toList());

    // Weighted choose by weight
    int maxWeight = prio.stream().mapToInt(EndpointConfig::getWeight).max().orElse(1);
    int minWeight = prio.stream().mapToInt(EndpointConfig::getWeight).min().orElse(1);
    List<EndpointConfig> weightFiltered;
    if (maxWeight == minWeight) {
      weightFiltered = prio;
    } else {
      int total = prio.stream().mapToInt(EndpointConfig::getWeight).sum();
      int rnd = new Random().nextInt(Math.max(1, total));
      int acc = 0;
      EndpointConfig picked = null;
      for (EndpointConfig ep : prio) {
        acc += Math.max(0, ep.getWeight());
        if (rnd < acc) { picked = ep; break; }
      }
      if (picked == null && !prio.isEmpty()) picked = prio.get(0);
      weightFiltered = picked == null ? prio : Collections.singletonList(picked);
    }

    // Tie-breaker
    if (weightFiltered.size() == 1) return Optional.of(weightFiltered.get(0));

    if (group.getSelection().getTieBreaker() == TieBreaker.LEAST_PLAYERS) {
      EndpointConfig best = null;
      int bestLoad = Integer.MAX_VALUE;
      for (EndpointConfig ep : weightFiltered) {
        int load = estimateLoad(plugin, ep);
        if (load < bestLoad) { bestLoad = load; best = ep; }
      }
      return Optional.ofNullable(best);
    }

    // No tie-breaker
    return Optional.of(weightFiltered.get(0));
  }

  private static boolean isOnline(PistonQueuePlugin plugin, LobbyGroupConfig group, EndpointConfig ep) {
    if (ep.getMode() == EndpointMode.VELOCITY) {
      if (ep.getVelocityServer() == null) return false;
      return plugin.getServer(ep.getVelocityServer()).map(ServerInfoWrapper::isOnline).orElse(false);
    }
    // TRANSFER – simple TCP connect probe
    if (ep.getHost() == null || ep.getPort() == 0) return false;
    int timeout = Math.max(100, group.getSelection().getPingTimeoutMs());
    try (Socket s = new Socket()) {
      s.connect(new InetSocketAddress(ep.getHost(), ep.getPort()), timeout);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  public static void noteTransfer(EndpointConfig ep, int cacheTtlMs) {
    String key = keyOf(ep);
    loadMap.compute(key, (k, v) -> {
      if (v == null) v = new LoadEntry();
      v.count++;
      v.expiresAt = Instant.now().toEpochMilli() + Math.max(500, cacheTtlMs);
      return v;
    });
  }

  private static int estimateLoad(PistonQueuePlugin plugin, EndpointConfig ep) {
    if ((ep.getMode() == EndpointMode.VELOCITY) && ep.getVelocityServer() != null) {
      return plugin.getServer(ep.getVelocityServer())
        .map(ServerInfoWrapper::getConnectedPlayers).map(List::size).orElse(Integer.MAX_VALUE / 2);
    }
    // TRANSFER – depending on configured player count source
    // Try status ping if configured; fallback to recent transfer counts
    if (ep.getHost() != null && ep.getPort() > 0) {
      // Determine group options not available here; default to AUTO behavior using status ping first
      Integer online = MinecraftStatusPinger.getOnlinePlayers(ep.getHost(), ep.getPort(), 700);
      if (online != null) return online;
    }
    LoadEntry entry = loadMap.get(keyOf(ep));
    if (entry == null) return Integer.MAX_VALUE / 3;
    if (entry.expiresAt < System.currentTimeMillis()) return Integer.MAX_VALUE / 3;
    return entry.count;
  }

  private static String keyOf(EndpointConfig ep) {
    return ep.getMode() + ":" + (ep.getMode() == EndpointMode.VELOCITY ? ep.getVelocityServer() : (ep.getHost() + ":" + ep.getPort()));
  }

  public static final class LoadEntry {
    int count = 0;
    long expiresAt = 0L;
  }
}
