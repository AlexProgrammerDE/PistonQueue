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
package net.pistonmaster.pistonqueue.shared.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public final class StorageData {
  @Comment({
    "Shadow banned players mapped to the date when they should be unbanned."
  })
  private Map<String, String> bans = new LinkedHashMap<>();

  public Map<String, String> getBans() {
    return Collections.unmodifiableMap(bans);
  }

  @SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Mutable map required for runtime updates"
  )
  public Map<String, String> getMutableBans() {
    return bans;
  }
}
