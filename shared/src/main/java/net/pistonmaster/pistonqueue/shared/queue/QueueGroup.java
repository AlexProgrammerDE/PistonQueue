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
package net.pistonmaster.pistonqueue.shared.queue;

import java.util.List;
import java.util.Locale;

public record QueueGroup(String name, List<String> queueServers, List<String> targetServers, List<String> sourceServers, List<QueueType> queueTypes) {
  public QueueGroup(String name, List<String> queueServers, List<String> targetServers, List<String> sourceServers, List<QueueType> queueTypes) {
    this.name = name;
    this.queueServers = List.copyOf(queueServers);
    this.targetServers = List.copyOf(targetServers);
    this.sourceServers = List.copyOf(sourceServers);
    this.queueTypes = queueTypes == null ? List.of() : List.copyOf(queueTypes);
  }

  /// Returns all queue servers configured for this group.
  ///
  /// @return an unmodifiable list of queue server names
  @Override
  public List<String> queueServers() {
    return queueServers;
  }

  /// Checks if the given server name is one of this group's queue servers.
  ///
  /// @param server the server name to check (case-insensitive)
  /// @return true if the server is a queue server for this group
  public boolean hasQueueServer(String server) {
    if (server == null) {
      return false;
    }
    String lowerServer = server.toLowerCase(Locale.ROOT);
    return queueServers.stream()
      .anyMatch(qs -> qs.toLowerCase(Locale.ROOT).equals(lowerServer));
  }
}
