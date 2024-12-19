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
package net.pistonmaster.pistonqueue.shared.events;

import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.Optional;

/**
 * Event for trying to connect to a server that allows us to intercept the connection and redirect the player.
 */
public interface PQServerPreConnectEvent {
    PlayerWrapper getPlayer();

    Optional<String> getTarget();

    void setTarget(String server);
}
