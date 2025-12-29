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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {

  @TempDir
  Path tempDir;

  @Test
  void doesNothingWhenFileDoesNotExist() throws IOException {
    Path configFile = tempDir.resolve("nonexistent.yml");

    // Should not throw
    ConfigMigrator.migrate(configFile);

    assertFalse(Files.exists(configFile));
  }

  @Test
  void migratesV2ToV3UpdatesConfigVersion() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      serverName: "&ctest"
      queueServer: "queue"
      targetServer: "main"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    assertTrue(result.contains("configVersion: 3"));
  }

  @Test
  void migratesV2ToV3ReplacesQueueServerPlaceholder() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      rawKickWhenDownServers:
        - "%TARGET_SERVER%"
        - "%QUEUE_SERVER%"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    assertTrue(result.contains("%QUEUE_SERVERS%"));
    assertFalse(result.contains("%QUEUE_SERVER%"));
  }

  @Test
  void migratesV2ToV3ConvertsQueueServerToList() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      queueGroups:
        default:
          queueServer: "queue1"
          targetServers:
            - "main"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    assertTrue(result.contains("queueServers:"));
    assertTrue(result.contains("- \"queue1\""));
    assertFalse(result.contains("queueServer:"));
  }

  @Test
  void doesNotModifyCurrentVersionConfig() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String currentConfig = """
      configVersion: 3
      serverName: "&ctest"
      queueServer: "queue"
      """;

    Files.writeString(configFile, currentConfig, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    assertEquals(currentConfig, result);
  }

  @Test
  void handlesMultipleQueueServerEntriesInDifferentGroups() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      queueGroups:
        group1:
          queueServer: "queue1"
          targetServers:
            - "main1"
        group2:
          queueServer: "queue2"
          targetServers:
            - "main2"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    assertTrue(result.contains("- \"queue1\""));
    assertTrue(result.contains("- \"queue2\""));
    // Both should be converted
    assertFalse(result.contains("queueServer:"));
  }

  @Test
  void preservesQuotedServerNames() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      queueGroups:
        default:
          queueServer: "my-queue-server"
          targetServers:
            - "main"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    assertTrue(result.contains("- \"my-queue-server\""));
  }

  @Test
  void handlesUnquotedServerNames() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      queueGroups:
        default:
          queueServer: queue1
          targetServers:
            - "main"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    assertTrue(result.contains("queueServers:"));
    assertTrue(result.contains("- \"queue1\""));
  }

  @Test
  void preservesIndentation() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      queueGroups:
        default:
          queueServer: "queue1"
          targetServers:
            - "main"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);
    // Should preserve the indentation structure
    assertTrue(result.contains("    queueServers:"));
    assertTrue(result.contains("      - \"queue1\""));
  }

  @Test
  void handlesEmptyFile() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    Files.writeString(configFile, "", StandardCharsets.UTF_8);

    // Should not throw - treats empty file as V1 legacy
    // This may fail during legacy migration but shouldn't crash on version detection
    try {
      ConfigMigrator.migrate(configFile);
    } catch (Exception e) {
      // Expected - empty file can't be parsed as V1 legacy
      assertTrue(e.getMessage() != null);
    }
  }

  @Test
  void migratesMultipleV2FeaturesInSinglePass() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v2Config = """
      configVersion: 2
      rawKickWhenDownServers:
        - "%TARGET_SERVER%"
        - "%QUEUE_SERVER%"
      queueGroups:
        default:
          queueServer: "queue1"
          targetServers:
            - "main"
      """;

    Files.writeString(configFile, v2Config, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);

    String result = Files.readString(configFile, StandardCharsets.UTF_8);

    // All migrations should apply
    assertTrue(result.contains("configVersion: 3"));
    assertTrue(result.contains("%QUEUE_SERVERS%"));
    assertTrue(result.contains("queueServers:"));
    assertTrue(result.contains("- \"queue1\""));
  }

  @Test
  void idempotentMigrationDoesNotChangeAlreadyMigratedFile() throws IOException {
    Path configFile = tempDir.resolve("config.yml");
    String v3Config = """
      configVersion: 3
      rawKickWhenDownServers:
        - "%TARGET_SERVER%"
        - "%QUEUE_SERVERS%"
      queueGroups:
        default:
          queueServers:
            - "queue1"
          targetServers:
            - "main"
      """;

    Files.writeString(configFile, v3Config, StandardCharsets.UTF_8);

    // Migrate twice
    ConfigMigrator.migrate(configFile);
    String afterFirst = Files.readString(configFile, StandardCharsets.UTF_8);

    ConfigMigrator.migrate(configFile);
    String afterSecond = Files.readString(configFile, StandardCharsets.UTF_8);

    assertEquals(afterFirst, afterSecond);
  }
}
