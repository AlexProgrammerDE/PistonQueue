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

import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.events.PQPreLoginEvent;

import java.util.Objects;

/**
 * Handles username validation for pre-login events.
 */
public final class UsernameValidator {
  private final Config config;

  public UsernameValidator(Config config) {
    this.config = Objects.requireNonNull(config, "config");
  }

  /**
   * Validates the username in the pre-login event and cancels it if it doesn't match the regex.
   *
   * @param event the pre-login event
   */
  public void validateUsername(PQPreLoginEvent event) {
    if (event.isCancelled()) {
      return;
    }

    if (config.enableUsernameRegex() && !event.getUsername().matches(config.usernameRegex())) {
      event.setCancelled(config.usernameRegexMessage().replace("%regex%", config.usernameRegex()));
    }
  }
}
