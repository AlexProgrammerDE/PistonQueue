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

import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

  @Test
  void getQueueTypeReturnsHighestPriorityTypePlayerHasPermissionFor() {
    Config config = createConfigWithMultipleQueueTypes();

    PlayerWrapper veteranPlayer = mockPlayer(Set.of("queue.veteran", "queue.priority"));
    PlayerWrapper priorityPlayer = mockPlayer(Set.of("queue.priority"));
    PlayerWrapper regularPlayer = mockPlayer(Set.of());

    QueueType veteranType = config.getQueueType(veteranPlayer);
    QueueType priorityType = config.getQueueType(priorityPlayer);
    QueueType regularType = config.getQueueType(regularPlayer);

    assertEquals("VETERAN", veteranType.getName());
    assertEquals("PRIORITY", priorityType.getName());
    assertEquals("REGULAR", regularType.getName());
  }

  @Test
  void getQueueTypeReturnsDefaultTypeWhenNoPermissions() {
    Config config = createConfigWithMultipleQueueTypes();
    PlayerWrapper player = mockPlayer(Set.of());

    QueueType type = config.getQueueType(player);

    assertEquals("REGULAR", type.getName());
    assertEquals("default", type.getPermission());
  }

  @Test
  void getQueueTypeThrowsWhenNoDefaultTypeAndNoMatchingPermission() {
    Config config = createConfigWithNoDefaultType();
    PlayerWrapper player = mockPlayer(Set.of());

    assertThrows(IllegalStateException.class, () -> config.getQueueType(player));
  }

  @Test
  void findGroupByTargetReturnsCaseInsensitive() {
    Config config = createConfigWithMultipleGroups();

    Optional<QueueGroup> lower = config.findGroupByTarget("main");
    Optional<QueueGroup> upper = config.findGroupByTarget("MAIN");
    Optional<QueueGroup> mixed = config.findGroupByTarget("MaIn");

    assertTrue(lower.isPresent());
    assertTrue(upper.isPresent());
    assertTrue(mixed.isPresent());
    assertEquals(lower.get(), upper.get());
    assertEquals(lower.get(), mixed.get());
  }

  @Test
  void findGroupByTargetReturnsEmptyForUnknownServer() {
    Config config = createConfigWithMultipleGroups();

    Optional<QueueGroup> result = config.findGroupByTarget("nonexistent");

    assertTrue(result.isEmpty());
  }

  @Test
  void findGroupByTargetReturnsEmptyForNull() {
    Config config = createConfigWithMultipleGroups();

    Optional<QueueGroup> result = config.findGroupByTarget(null);

    assertTrue(result.isEmpty());
  }

  @Test
  void kickWhenDownServersExpandsPlaceholders() {
    Config config = new Config();
    config.copyFrom(config);
    config.setEnableSourceServer(true);

    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of("lobby"));
    groupConfig.setQueueTypes(List.of());

    Map<String, Config.QueueGroupConfiguration> groupMap = new LinkedHashMap<>();
    groupMap.put("default", groupConfig);
    config.setQueueGroupDefinitions(groupMap);

    config.setRawKickWhenDownServers(List.of("%TARGET_SERVER%", "%QUEUE_SERVERS%", "%SOURCE_SERVER%"));

    List<String> servers = config.kickWhenDownServers();

    assertTrue(servers.contains("main"));
    assertTrue(servers.contains("queue"));
    assertTrue(servers.contains("lobby"));
  }

  @Test
  void kickWhenDownServersExpandsCommaSeparatedList() {
    Config config = new Config();
    config.copyFrom(config);

    config.setRawKickWhenDownServers(List.of("server1,server2,server3"));

    // Trigger rebuild
    config.copyFrom(config);

    List<String> servers = config.kickWhenDownServers();

    assertTrue(servers.contains("server1"));
    assertTrue(servers.contains("server2"));
    assertTrue(servers.contains("server3"));
  }

  @Test
  void kickWhenDownServersDeduplicates() {
    Config config = new Config();
    config.copyFrom(config);

    config.setRawKickWhenDownServers(List.of("server1", "server1", "server2"));

    // Trigger rebuild
    config.copyFrom(config);

    List<String> servers = config.kickWhenDownServers();

    long server1Count = servers.stream().filter(s -> s.equals("server1")).count();
    assertEquals(1, server1Count);
  }

  @Test
  void queueTypesAreSortedByOrder() {
    Config config = createConfigWithMultipleQueueTypes();

    List<QueueType> types = config.getAllQueueTypes();

    // VETERAN (priority 3) > PRIORITY (priority 2) > REGULAR (priority 1)
    assertEquals("VETERAN", types.get(0).getName());
    assertEquals("PRIORITY", types.get(1).getName());
    assertEquals("REGULAR", types.get(2).getName());
  }

  @Test
  void queueGroupsHaveCorrectQueueTypes() {
    Config config = createConfigWithMultipleGroups();

    Collection<QueueGroup> groups = config.getQueueGroups();
    QueueGroup defaultGroup = config.getDefaultGroup();

    assertNotNull(defaultGroup);
    assertFalse(groups.isEmpty());
    assertFalse(defaultGroup.queueTypes().isEmpty());
  }

  @Test
  void getGroupForReturnsCorrectGroup() {
    Config config = createConfigWithMultipleGroups();

    List<QueueType> types = config.getAllQueueTypes();
    for (QueueType type : types) {
      QueueGroup group = config.getGroupFor(type);
      assertNotNull(group, "Group should exist for type: " + type.getName());
    }
  }

  @Test
  void queueGroupWithMultipleQueueServers() {
    Config config = new Config();
    config.copyFrom(config);

    Config.QueueTypeConfiguration queueType = new Config.QueueTypeConfiguration();
    queueType.setPriority(1);
    queueType.setReservedSlots(50);
    queueType.setPermission("default");
    queueType.setHeader(List.of());
    queueType.setFooter(List.of());

    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue1", "queue2", "queue3"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of());
    groupConfig.setQueueTypes(List.of("DEFAULT"));

    Map<String, Config.QueueTypeConfiguration> typeMap = new LinkedHashMap<>();
    typeMap.put("DEFAULT", queueType);

    Map<String, Config.QueueGroupConfiguration> groupMap = new LinkedHashMap<>();
    groupMap.put("default", groupConfig);

    config.setQueueDefinitions(typeMap, groupMap);

    QueueGroup group = config.getDefaultGroup();
    assertNotNull(group);
    assertEquals(3, group.queueServers().size());
    assertTrue(group.queueServers().contains("queue1"));
    assertTrue(group.queueServers().contains("queue2"));
    assertTrue(group.queueServers().contains("queue3"));
  }

  @Test
  void queueGroupReferencingUnknownTypeThrows() {
    Config config = new Config();
    config.copyFrom(config);

    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of());
    groupConfig.setQueueTypes(List.of("NONEXISTENT_TYPE"));

    Map<String, Config.QueueTypeConfiguration> typeMap = new LinkedHashMap<>();

    Map<String, Config.QueueGroupConfiguration> groupMap = new LinkedHashMap<>();
    groupMap.put("default", groupConfig);

    assertThrows(IllegalStateException.class, () ->
      config.setQueueDefinitions(typeMap, groupMap)
    );
  }

  @Test
  void copyFromPreservesAllSettings() {
    Config source = new Config();
    source.copyFrom(source);
    source.setServerName("TestServer");
    source.setMaxPlayersPerMove(5);
    source.setPauseQueueIfTargetDown(false);

    Config target = new Config();
    target.copyFrom(source);

    assertEquals("TestServer", target.serverName());
    assertEquals(5, target.maxPlayersPerMove());
    assertFalse(target.pauseQueueIfTargetDown());
  }

  @Test
  void emptyQueueTypeDefinitionsUseDefaults() {
    Config config = new Config();
    config.copyFrom(config);

    // Default config should have queue types
    List<QueueType> types = config.getAllQueueTypes();

    assertFalse(types.isEmpty());
  }

  // Helper methods

  private Config createConfigWithMultipleQueueTypes() {
    Config config = new Config();
    config.copyFrom(config);

    Map<String, Config.QueueTypeConfiguration> typeMap = new LinkedHashMap<>();
    typeMap.put("VETERAN", createQueueType(3, 20, "queue.veteran"));
    typeMap.put("PRIORITY", createQueueType(2, 30, "queue.priority"));
    typeMap.put("REGULAR", createQueueType(1, 50, "default"));

    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of());
    groupConfig.setQueueTypes(List.of("VETERAN", "PRIORITY", "REGULAR"));

    Map<String, Config.QueueGroupConfiguration> groupMap = new LinkedHashMap<>();
    groupMap.put("default", groupConfig);

    config.setQueueDefinitions(typeMap, groupMap);

    return config;
  }

  private Config createConfigWithNoDefaultType() {
    Config config = new Config();
    config.copyFrom(config);

    Map<String, Config.QueueTypeConfiguration> typeMap = new LinkedHashMap<>();
    typeMap.put("VIP", createQueueType(1, 20, "queue.vip"));
    typeMap.put("PREMIUM", createQueueType(2, 30, "queue.premium"));

    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of());
    groupConfig.setQueueTypes(List.of("VIP", "PREMIUM"));

    Map<String, Config.QueueGroupConfiguration> groupMap = new LinkedHashMap<>();
    groupMap.put("default", groupConfig);

    config.setQueueDefinitions(typeMap, groupMap);

    return config;
  }

  private Config createConfigWithMultipleGroups() {
    Config config = new Config();
    config.copyFrom(config);

    Map<String, Config.QueueTypeConfiguration> typeMap = new LinkedHashMap<>();
    typeMap.put("REGULAR", createQueueType(1, 50, "default"));

    Config.QueueGroupConfiguration groupConfig = new Config.QueueGroupConfiguration();
    groupConfig.setDefaultGroup(true);
    groupConfig.setQueueServers(List.of("queue"));
    groupConfig.setTargetServers(List.of("main"));
    groupConfig.setSourceServers(List.of());
    groupConfig.setQueueTypes(List.of("REGULAR"));

    Map<String, Config.QueueGroupConfiguration> groupMap = new LinkedHashMap<>();
    groupMap.put("default", groupConfig);

    config.setQueueDefinitions(typeMap, groupMap);

    return config;
  }

  private Config.QueueTypeConfiguration createQueueType(int priority, int reservedSlots, String permission) {
    Config.QueueTypeConfiguration type = new Config.QueueTypeConfiguration();
    type.setPriority(priority);
    type.setReservedSlots(reservedSlots);
    type.setPermission(permission);
    type.setHeader(List.of());
    type.setFooter(List.of());
    return type;
  }

  private PlayerWrapper mockPlayer(Set<String> permissions) {
    return new PlayerWrapper() {
      @Override
      public boolean hasPermission(String node) {
        return permissions.contains(node);
      }

      @Override
      public void connect(String server) {}

      @Override
      public Optional<String> getCurrentServer() {
        return Optional.empty();
      }

      @Override
      public void sendMessage(net.pistonmaster.pistonqueue.shared.chat.MessageType type, String message) {}

      @Override
      public void sendPlayerList(List<String> header, List<String> footer) {}

      @Override
      public void resetPlayerList() {}

      @Override
      public String getName() {
        return "TestPlayer";
      }

      @Override
      public UUID getUniqueId() {
        return UUID.randomUUID();
      }

      @Override
      public void disconnect(String message) {}
    };
  }
}
