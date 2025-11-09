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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Centralizes access to frequently used queue context objects so that the
 * extracted logic can stay framework agnostic and easy to test.
 */
public final class QueueEnvironment {
  private final PistonQueuePlugin plugin;
  private final Supplier<Config> configSupplier;
  private final Set<String> onlineServers;

  public QueueEnvironment(PistonQueuePlugin plugin, Supplier<Config> configSupplier, Set<String> onlineServers) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
    this.onlineServers = Objects.requireNonNull(onlineServers, "onlineServers");
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Plugin API is immutable for consumers")
  public PistonQueuePlugin plugin() {
    return plugin;
  }

  public Config config() {
    return configSupplier.get();
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Caller needs live view for synchronization")
  public Set<String> onlineServers() {
    return onlineServers;
  }

  public QueueGroup defaultGroup() {
    Config config = config();
    QueueGroup group = config.getDefaultGroup();
    if (group != null) {
      return group;
    }

    QueueType[] queueTypes = config.getAllQueueTypes().toArray(new QueueType[0]);
    return new QueueGroup(
      "default",
      config.QUEUE_SERVER,
      List.of(config.TARGET_SERVER),
      config.ENABLE_SOURCE_SERVER ? List.of(config.SOURCE_SERVER) : List.of(),
      queueTypes
    );
  }

  public QueueGroup resolveGroupForTarget(String server) {
    if (server == null) {
      return defaultGroup();
    }
    return config().findGroupByTarget(server).orElse(defaultGroup());
  }

  public QueueGroup resolveGroupForType(QueueType type) {
    QueueGroup group = config().getGroupFor(type);
    return group != null ? group : defaultGroup();
  }

  public String defaultTarget(QueueGroup group) {
    if (group.getTargetServers().isEmpty()) {
      return config().TARGET_SERVER;
    }
    return group.getTargetServers().get(0);
  }

  public boolean isGroupTargetOnline(QueueGroup group) {
    return group.getTargetServers().stream().anyMatch(onlineServers::contains);
  }
}
