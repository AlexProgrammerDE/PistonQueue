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
package net.pistonmaster.pistonqueue.shared.hooks;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pistonmaster.pistonmotd.api.PlaceholderParser;
import net.pistonmaster.pistonmotd.api.PlaceholderUtil;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.config.Config;

import java.util.Locale;
import java.util.concurrent.locks.Lock;

public final class PistonMOTDPlaceholder implements PlaceholderParser {
  private final Config config;

  @SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Placeholder must reflect live configuration changes and only reads from the provided reference"
  )
  public PistonMOTDPlaceholder(Config config) {
    this.config = config;
    PlaceholderUtil.registerParser(this);
  }

  @Override
  public String parseString(String s) {
    for (QueueType type : config.getAllQueueTypes()) {
      s = s.replace("%pistonqueue_" + type.getName().toLowerCase(Locale.ROOT) + "%", String.valueOf(queueSize(type)));
    }
    return s;
  }

  private static int queueSize(QueueType type) {
    Lock readLock = type.getQueueLock().readLock();
    readLock.lock();
    try {
      return type.getQueueMap().size();
    } finally {
      readLock.unlock();
    }
  }
}
