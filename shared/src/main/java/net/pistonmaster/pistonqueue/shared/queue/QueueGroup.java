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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class QueueGroup {
  private final String name;
  private final String queueServer;
  private final List<String> targetServers;
  private final List<String> sourceServers;
  private final QueueType[] queueTypes;

  public QueueGroup(String name, String queueServer, List<String> targetServers, List<String> sourceServers, QueueType[] queueTypes) {
    this.name = name;
    this.queueServer = queueServer;
    this.targetServers = Collections.unmodifiableList(new ArrayList<>(targetServers));
    this.sourceServers = Collections.unmodifiableList(new ArrayList<>(sourceServers));
    this.queueTypes = queueTypes == null ? new QueueType[0] : queueTypes.clone();
  }

  public String getName() {
    return name;
  }

  public String getQueueServer() {
    return queueServer;
  }

  public List<String> getTargetServers() {
    return targetServers;
  }

  public List<String> getSourceServers() {
    return sourceServers;
  }

  public QueueType[] getQueueTypes() {
    return queueTypes.clone();
  }
}
