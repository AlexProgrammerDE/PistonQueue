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
package net.pistonmaster.pistonqueue.shared.wrapper;

import net.pistonmaster.pistonqueue.shared.chat.MessageType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerWrapper extends PermissibleWrapper {
  void connect(String server);

  Optional<String> getCurrentServer();

  default void sendMessage(String message) {
    sendMessage(MessageType.CHAT, message);
  }

  void sendMessage(MessageType type, String message);

  void sendPlayerList(List<String> header, List<String> footer);

  void resetPlayerList();

  String getName();

  UUID getUniqueId();

  void disconnect(String message);
}
