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

/**
 * Strategy for selecting which queue server to send players to when multiple
 * queue servers are configured for a queue group.
 */
public enum LoadBalancingStrategy {
  /**
   * Alternates between queue servers in order.
   */
  ROUND_ROBIN,

  /**
   * Sends players to the queue server with the fewest connected players.
   */
  LEAST_PLAYERS,

  /**
   * Randomly selects a queue server.
   */
  RANDOM
}
