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

import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Performs migrations of legacy configuration files to the new format.
 */
public final class ConfigMigrator {
  private ConfigMigrator() {
  }

  public static void migrate(Path file) throws IOException {
    if (!Files.exists(file)) {
      return;
    }

    String content = Files.readString(file, StandardCharsets.UTF_8);
    if (content.contains("configVersion:")) {
      return;
    }

    Config.ConfigLegacyV1 legacy = YamlConfigurations.load(
      file,
      Config.ConfigLegacyV1.class,
      builder -> builder.setNameFormatter(NameFormatters.IDENTITY)
    );

    Config migrated = Config.fromLegacy(legacy);
    YamlConfigurations.save(
      file,
      Config.class,
      migrated,
      builder -> builder.setNameFormatter(NameFormatters.IDENTITY)
    );
  }
}
