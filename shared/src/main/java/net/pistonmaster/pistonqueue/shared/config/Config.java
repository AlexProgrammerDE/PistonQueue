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
import de.exlll.configlib.Ignore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pistonmaster.pistonqueue.shared.queue.BanType;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Configuration
@SuppressFBWarnings(
  value = "MS_PKGPROTECT",
  justification = "Fields intentionally public for ConfigLib serialization compatibility"
)
public final class Config {
  @Comment("Placeholder for %server_name%")
  public String SERVER_NAME = "&cexample.org";

  @Comment("Username regex options")
  public boolean ENABLE_USERNAME_REGEX = true;
  public String USERNAME_REGEX = "[a-zA-Z0-9_]*";
  public String USERNAME_REGEX_MESSAGE = "&6[PQ] Invalid username please use: %regex%";

  @Comment("Tab auto completion")
  public boolean REGISTER_TAB = true;

  @Comment("Server is full message")
  public String SERVER_IS_FULL_MESSAGE = "%server_name% &6is full";

  @Comment("It is not recommended to decrease this number (milliseconds)")
  public int SERVER_ONLINE_CHECK_DELAY = 500;

  @Comment("Where to send the queue position message and what to send.")
  public boolean POSITION_MESSAGE_CHAT = true;
  public boolean POSITION_MESSAGE_HOT_BAR = false;
  public String QUEUE_POSITION = "&6Position in queue: &l%position%";
  public int POSITION_MESSAGE_DELAY = 10000;

  @Comment("Whether to show a custom player list header and footer (tab) with the queue position.")
  public boolean POSITION_PLAYER_LIST = true;

  @Comment("This is a message to hide the actual reason of why you are getting kicked from the server.")
  public boolean ENABLE_KICK_MESSAGE = false;
  public String KICK_MESSAGE = "&6You have lost connection to the server";

  @Comment("Failure protection for the queue")
  public boolean PAUSE_QUEUE_IF_TARGET_DOWN = true;
  public String PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE = "&6The main server is down. We will be back soon!";

  @Comment("When the servers are down should we prevent new players from joining the proxy?")
  public boolean KICK_WHEN_DOWN = false;
  public String SERVER_DOWN_KICK_MESSAGE = "%server_name% &6is down please try again later :(";

  @Comment("%TARGET_SERVER%, %QUEUE_SERVER% and %SOURCE_SERVER% are placeholders for the server names")
  public List<String> RAW_KICK_WHEN_DOWN_SERVERS = new ArrayList<>(List.of(
    "%TARGET_SERVER%",
    "%QUEUE_SERVER%"
  ));

  @Comment({
    "If a player gets kicked from the target server (it goes down/crashes),",
    "they gets sent into queue and waits."
  })
  public boolean IF_TARGET_DOWN_SEND_TO_QUEUE = true;
  public String IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE = "&cThe target server is offline now! You have been sent to queue while it goes back online.";

  @Comment("Only send players back to queue if the kick message has these words. (lowercase)")
  public List<String> DOWN_WORD_LIST = new ArrayList<>(List.of(
    "restarting",
    "closed",
    "went down",
    "unknown reason"
  ));

  @Comment("If something went wrong while queue -> target happens should the player be sent back into queue?")
  public boolean RECOVERY = true;
  public String RECOVERY_MESSAGE = "&cOops something went wrong... Putting you back in queue.";

  @Comment("Set the queue servers name that is in the proxy config file.")
  public String QUEUE_SERVER = "queue";
  public String TARGET_SERVER = "main";

  public boolean FORCE_TARGET_SERVER = false;

  @Comment({
    "Set this to true if you're a lobby or cracked/offline mode server.",
    "This option is required for those setups to work. Make your proxy sends source -> target."
  })
  public boolean ENABLE_SOURCE_SERVER = false;
  public String SOURCE_SERVER = "lobby";

  @Comment("Connecting to server message")
  public String JOINING_TARGET_SERVER = "&6Connecting to the server...";

  @Comment("Total people allowed to be connected to the queue network")
  public int QUEUE_SERVER_SLOTS = 9000;

  @Comment("Queue move delay in milliseconds default is 1000ms")
  public int QUEUE_MOVE_DELAY = 1000;

  @Comment("Max players allowed to be moved into the target server at once with one queue move.")
  public int MAX_PLAYERS_PER_MOVE = 10;

  @Comment("Should the queue be always active or only when the target server is full?")
  public boolean ALWAYS_QUEUE = false;

  @Comment("Send an XP sound to every player who gets to position 5 or below in the queue")
  public boolean SEND_XP_SOUND = true;

  @Comment("The way shadow-bans should work on your server.")
  public BanType SHADOW_BAN_TYPE = BanType.LOOP;
  @Comment("Custom percentage for the shadow-ban type. Only used when SHADOW_BAN_TYPE is set to PERCENT.")
  public int PERCENT = 10;
  public String SHADOW_BAN_MESSAGE = "&6You have lost connection to the server";

  @Comment("Permissions")
  public String QUEUE_BYPASS_PERMISSION = "queue.bypass";
  public String ADMIN_PERMISSION = "queue.admin";

  @Comment("Adding or removing queue types requires a full restart!!!")
  private Map<String, QueueTypeConfiguration> QUEUE_TYPE_DEFINITIONS = defaultQueueTypes();

  @Ignore
  public List<String> KICK_WHEN_DOWN_SERVERS = new ArrayList<>();

  @Ignore
  public QueueType[] QUEUE_TYPES; // Not allowed to be resized due to data corruption

  @Ignore
  private final List<QueueGroup> queueGroups = new ArrayList<>();

  @Ignore
  private final Map<String, QueueGroup> queueGroupsByName = new LinkedHashMap<>();

  @Ignore
  private final Map<String, QueueGroup> queueGroupsByTarget = new HashMap<>();

  @Ignore
  private final IdentityHashMap<QueueType, QueueGroup> queueGroupByType = new IdentityHashMap<>();

  @Ignore
  private final List<QueueType> allQueueTypes = new ArrayList<>();

  @Ignore
  private QueueGroup defaultQueueGroup;

  public Config() {
  }

  public void copyFrom(Config source) {
    SERVER_NAME = source.SERVER_NAME;
    ENABLE_USERNAME_REGEX = source.ENABLE_USERNAME_REGEX;
    USERNAME_REGEX = source.USERNAME_REGEX;
    USERNAME_REGEX_MESSAGE = source.USERNAME_REGEX_MESSAGE;
    REGISTER_TAB = source.REGISTER_TAB;
    SERVER_IS_FULL_MESSAGE = source.SERVER_IS_FULL_MESSAGE;
    SERVER_ONLINE_CHECK_DELAY = source.SERVER_ONLINE_CHECK_DELAY;
    POSITION_MESSAGE_CHAT = source.POSITION_MESSAGE_CHAT;
    POSITION_MESSAGE_HOT_BAR = source.POSITION_MESSAGE_HOT_BAR;
    QUEUE_POSITION = source.QUEUE_POSITION;
    POSITION_MESSAGE_DELAY = source.POSITION_MESSAGE_DELAY;
    POSITION_PLAYER_LIST = source.POSITION_PLAYER_LIST;
    ENABLE_KICK_MESSAGE = source.ENABLE_KICK_MESSAGE;
    KICK_MESSAGE = source.KICK_MESSAGE;
    PAUSE_QUEUE_IF_TARGET_DOWN = source.PAUSE_QUEUE_IF_TARGET_DOWN;
    PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE = source.PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE;
    KICK_WHEN_DOWN = source.KICK_WHEN_DOWN;
    RAW_KICK_WHEN_DOWN_SERVERS = new ArrayList<>(source.RAW_KICK_WHEN_DOWN_SERVERS);
    SERVER_DOWN_KICK_MESSAGE = source.SERVER_DOWN_KICK_MESSAGE;
    IF_TARGET_DOWN_SEND_TO_QUEUE = source.IF_TARGET_DOWN_SEND_TO_QUEUE;
    IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE = source.IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE;
    DOWN_WORD_LIST = new ArrayList<>(source.DOWN_WORD_LIST);
    RECOVERY = source.RECOVERY;
    RECOVERY_MESSAGE = source.RECOVERY_MESSAGE;
    QUEUE_SERVER = source.QUEUE_SERVER;
    TARGET_SERVER = source.TARGET_SERVER;
    FORCE_TARGET_SERVER = source.FORCE_TARGET_SERVER;
    ENABLE_SOURCE_SERVER = source.ENABLE_SOURCE_SERVER;
    SOURCE_SERVER = source.SOURCE_SERVER;
    JOINING_TARGET_SERVER = source.JOINING_TARGET_SERVER;
    QUEUE_SERVER_SLOTS = source.QUEUE_SERVER_SLOTS;
    QUEUE_MOVE_DELAY = source.QUEUE_MOVE_DELAY;
    MAX_PLAYERS_PER_MOVE = source.MAX_PLAYERS_PER_MOVE;
    ALWAYS_QUEUE = source.ALWAYS_QUEUE;
    SEND_XP_SOUND = source.SEND_XP_SOUND;
    SHADOW_BAN_TYPE = source.SHADOW_BAN_TYPE;
    PERCENT = source.PERCENT;
    SHADOW_BAN_MESSAGE = source.SHADOW_BAN_MESSAGE;
    QUEUE_BYPASS_PERMISSION = source.QUEUE_BYPASS_PERMISSION;
    ADMIN_PERMISSION = source.ADMIN_PERMISSION;

    List<String> resolvedKickServers = new ArrayList<>();
    for (String text : source.RAW_KICK_WHEN_DOWN_SERVERS) {
      resolvedKickServers.add(text
        .replace("%TARGET_SERVER%", TARGET_SERVER)
        .replace("%QUEUE_SERVER%", QUEUE_SERVER)
        .replace("%SOURCE_SERVER%", SOURCE_SERVER));
    }
    if (!resolvedKickServers.contains(TARGET_SERVER)) {
      resolvedKickServers.add(TARGET_SERVER);
    }
    KICK_WHEN_DOWN_SERVERS = resolvedKickServers;

    QUEUE_TYPE_DEFINITIONS = copyQueueDefinitions(source.QUEUE_TYPE_DEFINITIONS);
    applyQueueTypes(QUEUE_TYPE_DEFINITIONS);
    rebuildQueueGroups();
  }

  public QueueType getQueueType(PlayerWrapper player) {
    for (QueueType type : allQueueTypes) {
      if (type.getPermission().equals("default") || player.hasPermission(type.getPermission())) {
        return type;
      }
    }
    throw new RuntimeException("No queue type found for player! (There is no default queue type)");
  }

  private void applyQueueTypes(Map<String, QueueTypeConfiguration> queueTypeConfiguration) {
    if (QUEUE_TYPES == null) {
      QUEUE_TYPES = queueTypeConfiguration.entrySet().stream()
        .map(entry -> buildQueueType(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparingInt(QueueType::getOrder))
        .toArray(QueueType[]::new);
      return;
    }

    for (QueueType queueType : QUEUE_TYPES) {
      QueueTypeConfiguration configuration = queueTypeConfiguration.get(queueType.getName());
      if (configuration == null) {
        continue;
      }
      queueType.setOrder(configuration.getOrder());
      queueType.setPermission(configuration.getPermission());
      queueType.setReservedSlots(configuration.getSlots());
      queueType.setHeader(new ArrayList<>(configuration.getHeader()));
      queueType.setFooter(new ArrayList<>(configuration.getFooter()));
    }
    Arrays.sort(QUEUE_TYPES, Comparator.comparingInt(QueueType::getOrder));
  }

  private static QueueType buildQueueType(String name, QueueTypeConfiguration configuration) {
    return new QueueType(
      name,
      configuration.getOrder(),
      configuration.getPermission(),
      configuration.getSlots(),
      new ArrayList<>(configuration.getHeader()),
      new ArrayList<>(configuration.getFooter()));
  }

  private static Map<String, QueueTypeConfiguration> defaultQueueTypes() {
    Map<String, QueueTypeConfiguration> defaults = new LinkedHashMap<>();
    defaults.put("REGULAR", createQueueType(
      3,
      50,
      "default",
      new ArrayList<>(List.of(
        "",
        " %server_name% ",
        "",
        " %server_name% &bis full ",
        " Position in queue: &l%position% ",
        " &6Estimated time: &l%wait% ",
        ""
      )),
      new ArrayList<>(List.of(
        "",
        " &6You can now donate to receive priority queue status, please visit donate.example.com ",
        "",
        " &7contact: contact@example.com ",
        " discussion: https://discord.example.com ",
        " website: https://www.example.com ",
        " These are the only official %server_name% websites and contacts ",
        ""
      ))
    ));
    defaults.put("PRIORITY", createQueueType(
      2,
      30,
      "queue.priority",
      new ArrayList<>(List.of(
        "",
        " %server_name%",
        "",
        " %server_name% &bis full ",
        " Position in queue: &l%position% ",
        " &6Estimated time: &l%wait% ",
        ""
      )),
      new ArrayList<>(List.of(
        "",
        " &6You have priority queue status please wait for an available slot on the server ",
        "",
        " &7contact: contact@example.com ",
        " discussion: https://discord.example.com ",
        " website: https://www.example.com ",
        " These are the only official %server_name% websites and contacts ",
        ""
      ))
    ));
    defaults.put("VETERAN", createQueueType(
      1,
      20,
      "queue.veteran",
      new ArrayList<>(List.of(
        "",
        " %server_name% ",
        "",
        " %server_name% &bis full ",
        " Position in queue: &l%position% ",
        " &6Estimated time: &l%wait% ",
        ""
      )),
      new ArrayList<>(List.of(
        "",
        " &6You have veteran queue status please wait for an available slot on the server ",
        "",
        " &7contact: contact@example.com ",
        " discussion: https://discord.example.com ",
        " website: https://www.example.com ",
        " These are the only official %server_name% websites and contacts ",
        ""
      ))
    ));
    return defaults;
  }

  private static QueueTypeConfiguration createQueueType(
    int order,
    int slots,
    String permission,
    List<String> header,
    List<String> footer
  ) {
    QueueTypeConfiguration configuration = new QueueTypeConfiguration();
    configuration.ORDER = order;
    configuration.SLOTS = slots;
    configuration.PERMISSION = permission;
    configuration.HEADER = header;
    configuration.FOOTER = footer;
    return configuration;
  }

  private static Map<String, QueueTypeConfiguration> copyQueueDefinitions(Map<String, QueueTypeConfiguration> original) {
    Map<String, QueueTypeConfiguration> copy = new LinkedHashMap<>();
    original.forEach((name, def) -> copy.put(name, def.copy()));
    return copy;
  }

  public QueueGroup getDefaultGroup() {
    return defaultQueueGroup;
  }

  public Collection<QueueGroup> getQueueGroups() {
    return Collections.unmodifiableList(queueGroups);
  }

  public Optional<QueueGroup> findGroupByTarget(String server) {
    if (server == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(queueGroupsByTarget.get(server.toLowerCase(Locale.ROOT)));
  }

  public QueueGroup getGroupFor(QueueType type) {
    return queueGroupByType.get(type);
  }

  public List<QueueType> getAllQueueTypes() {
    return Collections.unmodifiableList(allQueueTypes);
  }

  private void rebuildQueueGroups() {
    queueGroups.clear();
    queueGroupsByName.clear();
    queueGroupsByTarget.clear();
    queueGroupByType.clear();
    allQueueTypes.clear();

    QueueType[] effectiveTypes = QUEUE_TYPES;
    if (effectiveTypes == null) {
      effectiveTypes = new QueueType[0];
      QUEUE_TYPES = effectiveTypes;
    }

    QueueGroup defaultGroup = new QueueGroup(
      "default",
      QUEUE_SERVER,
      Collections.singletonList(TARGET_SERVER),
      ENABLE_SOURCE_SERVER ? Collections.singletonList(SOURCE_SERVER) : Collections.emptyList(),
      effectiveTypes
    );
    registerGroup(defaultGroup);
    defaultQueueGroup = defaultGroup;
  }

  private void registerGroup(QueueGroup group) {
    queueGroups.add(group);
    queueGroupsByName.put(group.getName().toLowerCase(Locale.ROOT), group);
    for (String target : group.getTargetServers()) {
      queueGroupsByTarget.put(target.toLowerCase(Locale.ROOT), group);
    }
    for (QueueType type : group.getQueueTypes()) {
      queueGroupByType.put(type, group);
      allQueueTypes.add(type);
    }
  }

  @Configuration
  public static final class QueueTypeConfiguration {
    private int ORDER = 1;
    private int SLOTS = 0;
    private String PERMISSION = "default";
    private List<String> HEADER = new ArrayList<>();
    private List<String> FOOTER = new ArrayList<>();

    public int getOrder() {
      return ORDER;
    }

    public int getSlots() {
      return SLOTS;
    }

    public String getPermission() {
      return PERMISSION;
    }

    public List<String> getHeader() {
      return new ArrayList<>(HEADER);
    }

    public List<String> getFooter() {
      return new ArrayList<>(FOOTER);
    }

    private QueueTypeConfiguration copy() {
      QueueTypeConfiguration configuration = new QueueTypeConfiguration();
      configuration.ORDER = ORDER;
      configuration.SLOTS = SLOTS;
      configuration.PERMISSION = PERMISSION;
      configuration.HEADER = new ArrayList<>(HEADER);
      configuration.FOOTER = new ArrayList<>(FOOTER);
      return configuration;
    }
  }
}
