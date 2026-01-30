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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
@AllArgsConstructor
public class QueueType {
  private final Map<UUID, QueuedPlayer> queueMap = new LinkedHashMap<>();
  private final Map<Integer, Duration> durationFromPosition = new LinkedHashMap<>();
  private final ReadWriteLock queueLock = new ReentrantReadWriteLock();
  private final ReadWriteLock durationLock = new ReentrantReadWriteLock();
  private final Map<UUID, Map<Integer, Instant>> positionCache = new ConcurrentHashMap<>();
  private final Set<UUID> activeTransfers = ConcurrentHashMap.newKeySet();
  private final AtomicInteger playersWithTypeInTarget = new AtomicInteger();
  private final String name;
  @Setter
  private volatile int priority;
  @Setter
  private String permission;
  @Setter
  private volatile int reservedSlots;
  @Setter
  private List<String> header;
  @Setter
  private List<String> footer;

  public enum QueueReason {
    SERVER_FULL,
    SERVER_DOWN,
    RECOVERY
  }

  public record QueuedPlayer(
    String targetServer,
    QueueReason queueReason
  ) {}
}
