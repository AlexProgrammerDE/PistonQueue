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
package net.pistonmaster.pistonqueue.shared;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerWrapper {
    boolean hasPermission(String node);
    void connect(String server);
    Optional<String> getCurrentServer();
    void sendMessage(String message);
    void sendActionBar(String message);
    void sendPlayerListHeaderAndFooter(List<String> header, List<String> footer);
    UUID getUniqueId();
}
