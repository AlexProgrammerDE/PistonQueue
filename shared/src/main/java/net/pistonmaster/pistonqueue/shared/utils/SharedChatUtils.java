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
package net.pistonmaster.pistonqueue.shared.utils;

import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;

import java.time.Duration;
import java.util.Locale;

public final class SharedChatUtils {
  private SharedChatUtils() {
  }

  public static String formatDuration(String str, Duration duration, int position) {
    String format = duration.toHours() == 0 ?
      String.format("%dm", duration.toMinutes() == 0 ? 1 : duration.toMinutes()) :
      String.format("%dh %dm", duration.toHours(), duration.toMinutes() % 60);

    return str.replace("%position%", String.valueOf(position)).replace("%wait%", format);
  }

  public static String parseText(String text) {
    text = text.replace("%server_name%", Config.SERVER_NAME);
    for (QueueType type : Config.QUEUE_TYPES) {
      text = text.replace("%" + type.getName().toLowerCase(Locale.ROOT) + "%", String.valueOf(type.getQueueMap().size()));
    }
    text = text.replace("%position%", "None");
    text = text.replace("%wait%", "None");

    return text;
  }
}
