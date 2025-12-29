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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Performs migrations of legacy configuration files to the new format.
public final class ConfigMigrator {
  private static final Pattern CONFIG_VERSION_PATTERN = Pattern.compile("configVersion:\\s*(\\d+)");
  private static final Pattern QUEUE_SERVER_PATTERN = Pattern.compile("(\\s+)queueServer:\\s*\"?([^\"\\n]+)\"?");

  private ConfigMigrator() {
  }

  public static void migrate(Path file) throws IOException {
    if (!Files.exists(file)) {
      return;
    }

    String content = Files.readString(file, StandardCharsets.UTF_8);

    // Check if this is a legacy V1 config (no configVersion field)
    if (!content.contains("configVersion:")) {
      migrateFromV1(file);
      return;
    }

    // Check version and migrate as needed
    Matcher versionMatcher = CONFIG_VERSION_PATTERN.matcher(content);
    if (versionMatcher.find()) {
      int version = Integer.parseInt(versionMatcher.group(1));
      if (version < Config.CURRENT_VERSION) {
        content = migrateContent(content, version);
        Files.writeString(file, content, StandardCharsets.UTF_8);
      }
    }
  }

  private static void migrateFromV1(Path file) throws IOException {
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

  private static String migrateContent(String content, int fromVersion) {
    String result = content;

    // Migrate from V2 to V3
    if (fromVersion < 3) {
      result = migrateV2ToV3(result);
    }

    return result;
  }

  private static String migrateV2ToV3(String content) {
    String result = content;

    // Update configVersion to 3
    result = result.replaceFirst("configVersion:\\s*2", "configVersion: 3");

    // Migrate %QUEUE_SERVER% to %QUEUE_SERVERS% in rawKickWhenDownServers
    result = result.replace("%QUEUE_SERVER%", "%QUEUE_SERVERS%");

    // Migrate queueServer to queueServers in queue groups
    // This regex finds queueServer entries within queueGroups section and converts them to queueServers list
    Matcher matcher = QUEUE_SERVER_PATTERN.matcher(result);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String indent = matcher.group(1);
      String serverName = matcher.group(2).trim();
      // Convert to list format
      String replacement = indent + "queueServers:\n" + indent + "  - \"" + serverName + "\"";
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    result = sb.toString();

    return result;
  }
}
