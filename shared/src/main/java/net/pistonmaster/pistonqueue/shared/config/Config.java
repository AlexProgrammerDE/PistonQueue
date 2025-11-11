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
import de.exlll.configlib.PostProcess;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pistonmaster.pistonqueue.shared.queue.BanType;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Configuration
@SuppressFBWarnings(
  value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
  justification = "Fields intentionally exposed through getters for ConfigLib serialization compatibility"
)
public final class Config {
  public static final int CURRENT_VERSION = 2;

  @Comment("Configuration version. Do not edit manually.")
  private int configVersion = CURRENT_VERSION;

  @Comment("Placeholder for %server_name%")
  private String serverName = "&cexample.org";

  @Comment("Username regex options")
  private boolean enableUsernameRegex = true;
  private String usernameRegex = "[a-zA-Z0-9_]*";
  private String usernameRegexMessage = "&6[PQ] Invalid username please use: %regex%";

  @Comment("Tab auto completion")
  private boolean registerTab = true;

  @Comment("Server is full message")
  private String serverIsFullMessage = "%server_name% &6is full";

  @Comment("It is not recommended to decrease this number (milliseconds)")
  private int serverOnlineCheckDelay = 500;

  @Comment("Where to send the queue position message and what to send.")
  private boolean positionMessageChat = true;
  private boolean positionMessageHotBar = false;
  private String queuePosition = "&6Position in queue: &l%position%";
  private int positionMessageDelay = 10000;

  @Comment("Whether to show a custom player list header and footer (tab) with the queue position.")
  private boolean positionPlayerList = true;

  @Comment("This is a message to hide the actual reason of why you are getting kicked from the server.")
  private boolean enableKickMessage = false;
  private String kickMessage = "&6You have lost connection to the server";

  @Comment("Failure protection for the queue")
  private boolean pauseQueueIfTargetDown = true;
  private String pauseQueueIfTargetDownMessage = "&6The main server is down. We will be back soon!";

  @Comment("When the servers are down should we prevent new players from joining the proxy?")
  private boolean kickWhenDown = false;
  private String serverDownKickMessage = "%server_name% &6is down please try again later :(";

  @Comment("%TARGET_SERVER%, %QUEUE_SERVER% and %SOURCE_SERVER% are placeholders for the server names")
  private List<String> rawKickWhenDownServers = new ArrayList<>(List.of(
    "%TARGET_SERVER%",
    "%QUEUE_SERVER%"
  ));

  @Comment({
    "If a player gets kicked from the target server (it goes down/crashes),",
    "they gets sent into queue and waits."
  })
  private boolean ifTargetDownSendToQueue = true;
  private String ifTargetDownSendToQueueMessage = "&cThe target server is offline now! You have been sent to queue while it goes back online.";

  @Comment("Only send players back to queue if the kick message has these words. (lowercase)")
  private List<String> downWordList = new ArrayList<>(List.of(
    "restarting",
    "closed",
    "went down",
    "unknown reason"
  ));

  @Comment("If something went wrong while queue -> target happens should the player be sent back into queue?")
  private boolean recovery = true;
  private String recoveryMessage = "&cOops something went wrong... Putting you back in queue.";

  @Comment("Set the queue servers name that is in the proxy config file.")
  private String queueServer = "queue";
  private String targetServer = "main";

  private boolean forceTargetServer = false;

  @Comment({
    "Set this to true if you're a lobby or cracked/offline mode server.",
    "This option is required for those setups to work. Make your proxy sends source -> target."
  })
  private boolean enableSourceServer = false;
  private String sourceServer = "lobby";

  @Comment("Connecting to server message")
  private String joiningTargetServer = "&6Connecting to the server...";

  @Comment("Total people allowed to be connected to the queue network")
  private int queueServerSlots = 9000;

  @Comment("Queue move delay in milliseconds default is 1000ms")
  private int queueMoveDelay = 1000;

  @Comment("Max players allowed to be moved into the target server at once with one queue move.")
  private int maxPlayersPerMove = 10;

  @Comment("Should the queue be always active or only when the target server is full?")
  private boolean alwaysQueue = false;

  @Comment("Send an XP sound to every player who gets to position 5 or below in the queue")
  private boolean sendXpSound = true;

  @Comment("The way shadow-bans should work on your server.")
  private BanType shadowBanType = BanType.LOOP;
  @Comment("Custom percentage for the shadow-ban type. Only used when shadowBanType is set to PERCENT.")
  private int shadowBanPercent = 10;
  private String shadowBanMessage = "&6You have lost connection to the server";

  @Comment("Permissions")
  private String queueBypassPermission = "queue.bypass";
  private String adminPermission = "queue.admin";

  @Comment("Definitions for queue types. You can add or remove entries freely.")
  private Map<String, QueueTypeConfiguration> queueTypes = defaultQueueTypes();

  @Comment("Definitions for queue groups. Each group can point at any queue types.")
  private Map<String, QueueGroupConfiguration> queueGroups = defaultQueueGroups();

  @Ignore
  private List<String> kickWhenDownServers = new ArrayList<>();

  @Ignore
  private final Map<String, QueueType> queueTypeInstances = new LinkedHashMap<>();

  @Ignore
  private final List<QueueType> orderedQueueTypes = new ArrayList<>();

  @Ignore
  private final List<QueueGroup> queueGroupList = new ArrayList<>();

  @Ignore
  private final Map<String, QueueGroup> queueGroupsByName = new LinkedHashMap<>();

  @Ignore
  private final Map<String, QueueGroup> queueGroupsByTarget = new LinkedHashMap<>();

  @Ignore
  private final IdentityHashMap<QueueType, QueueGroup> queueGroupByType = new IdentityHashMap<>();

  @Ignore
  private QueueGroup defaultQueueGroup;

  public Config() {
  }

  @PostProcess
  private Config postProcess() {
    rebuildKickWhenDownServers();
    rebuildQueueTypes();
    rebuildQueueGroups();
    return this;
  }

  public void copyFrom(Config source) {
    configVersion = source.configVersion;
    serverName = source.serverName;
    enableUsernameRegex = source.enableUsernameRegex;
    usernameRegex = source.usernameRegex;
    usernameRegexMessage = source.usernameRegexMessage;
    registerTab = source.registerTab;
    serverIsFullMessage = source.serverIsFullMessage;
    serverOnlineCheckDelay = source.serverOnlineCheckDelay;
    positionMessageChat = source.positionMessageChat;
    positionMessageHotBar = source.positionMessageHotBar;
    queuePosition = source.queuePosition;
    positionMessageDelay = source.positionMessageDelay;
    positionPlayerList = source.positionPlayerList;
    enableKickMessage = source.enableKickMessage;
    kickMessage = source.kickMessage;
    pauseQueueIfTargetDown = source.pauseQueueIfTargetDown;
    pauseQueueIfTargetDownMessage = source.pauseQueueIfTargetDownMessage;
    kickWhenDown = source.kickWhenDown;
    serverDownKickMessage = source.serverDownKickMessage;
    rawKickWhenDownServers = new ArrayList<>(source.rawKickWhenDownServers);
    ifTargetDownSendToQueue = source.ifTargetDownSendToQueue;
    ifTargetDownSendToQueueMessage = source.ifTargetDownSendToQueueMessage;
    downWordList = new ArrayList<>(source.downWordList);
    recovery = source.recovery;
    recoveryMessage = source.recoveryMessage;
    queueServer = source.queueServer;
    targetServer = source.targetServer;
    forceTargetServer = source.forceTargetServer;
    enableSourceServer = source.enableSourceServer;
    sourceServer = source.sourceServer;
    joiningTargetServer = source.joiningTargetServer;
    queueServerSlots = source.queueServerSlots;
    queueMoveDelay = source.queueMoveDelay;
    maxPlayersPerMove = source.maxPlayersPerMove;
    alwaysQueue = source.alwaysQueue;
    sendXpSound = source.sendXpSound;
    shadowBanType = source.shadowBanType;
    shadowBanPercent = source.shadowBanPercent;
    shadowBanMessage = source.shadowBanMessage;
    queueBypassPermission = source.queueBypassPermission;
    adminPermission = source.adminPermission;
    queueTypes = copyQueueDefinitions(source.queueTypes);
    queueGroups = copyGroupDefinitions(source.queueGroups);
    postProcess();
  }

  public int configVersion() {
    return configVersion;
  }

  public String serverName() {
    return serverName;
  }

  public boolean enableUsernameRegex() {
    return enableUsernameRegex;
  }

  public String usernameRegex() {
    return usernameRegex;
  }

  public String usernameRegexMessage() {
    return usernameRegexMessage;
  }

  public boolean registerTab() {
    return registerTab;
  }

  public String serverIsFullMessage() {
    return serverIsFullMessage;
  }

  public int serverOnlineCheckDelay() {
    return serverOnlineCheckDelay;
  }

  public boolean positionMessageChat() {
    return positionMessageChat;
  }

  public boolean positionMessageHotBar() {
    return positionMessageHotBar;
  }

  public String queuePosition() {
    return queuePosition;
  }

  public int positionMessageDelay() {
    return positionMessageDelay;
  }

  public boolean positionPlayerList() {
    return positionPlayerList;
  }

  public boolean enableKickMessage() {
    return enableKickMessage;
  }

  public String kickMessage() {
    return kickMessage;
  }

  public boolean pauseQueueIfTargetDown() {
    return pauseQueueIfTargetDown;
  }

  public String pauseQueueIfTargetDownMessage() {
    return pauseQueueIfTargetDownMessage;
  }

  public boolean kickWhenDown() {
    return kickWhenDown;
  }

  public String serverDownKickMessage() {
    return serverDownKickMessage;
  }

  public List<String> kickWhenDownServers() {
    return Collections.unmodifiableList(kickWhenDownServers);
  }

  public boolean ifTargetDownSendToQueue() {
    return ifTargetDownSendToQueue;
  }

  public String ifTargetDownSendToQueueMessage() {
    return ifTargetDownSendToQueueMessage;
  }

  public List<String> downWordList() {
    return Collections.unmodifiableList(downWordList);
  }

  public boolean recovery() {
    return recovery;
  }

  public String recoveryMessage() {
    return recoveryMessage;
  }

  public String queueServer() {
    return queueServer;
  }

  public String targetServer() {
    return targetServer;
  }

  public boolean forceTargetServer() {
    return forceTargetServer;
  }

  public boolean enableSourceServer() {
    return enableSourceServer;
  }

  public String sourceServer() {
    return sourceServer;
  }

  public String joiningTargetServer() {
    return joiningTargetServer;
  }

  public int queueServerSlots() {
    return queueServerSlots;
  }

  public int queueMoveDelay() {
    return queueMoveDelay;
  }

  public int maxPlayersPerMove() {
    return maxPlayersPerMove;
  }

  public boolean alwaysQueue() {
    return alwaysQueue;
  }

  public boolean sendXpSound() {
    return sendXpSound;
  }

  public BanType shadowBanType() {
    return shadowBanType;
  }

  public int shadowBanPercent() {
    return shadowBanPercent;
  }

  public String shadowBanMessage() {
    return shadowBanMessage;
  }

  public String queueBypassPermission() {
    return queueBypassPermission;
  }

  public String adminPermission() {
    return adminPermission;
  }

  public Map<String, QueueTypeConfiguration> queueTypeDefinitions() {
    Map<String, QueueTypeConfiguration> copy = new LinkedHashMap<>();
    queueTypes.forEach((name, def) -> copy.put(name, def.copy()));
    return copy;
  }

  public Map<String, QueueGroupConfiguration> queueGroupDefinitions() {
    Map<String, QueueGroupConfiguration> copy = new LinkedHashMap<>();
    queueGroups.forEach((name, def) -> copy.put(name, def.copy()));
    return copy;
  }

  public QueueType getQueueType(PlayerWrapper player) {
    for (QueueType type : orderedQueueTypes) {
      if ("default".equals(type.getPermission()) || player.hasPermission(type.getPermission())) {
        return type;
      }
    }
    throw new IllegalStateException("No queue type found for player! (There is no default queue type)");
  }

  public QueueGroup getDefaultGroup() {
    return defaultQueueGroup;
  }

  public Collection<QueueGroup> getQueueGroups() {
    return Collections.unmodifiableList(queueGroupList);
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
    return Collections.unmodifiableList(orderedQueueTypes);
  }

  public void setQueueTypeDefinitions(Map<String, QueueTypeConfiguration> definitions) {
    queueTypes = copyQueueDefinitions(definitions);
    rebuildQueueTypes();
    rebuildQueueGroups();
  }

  public void setQueueGroupDefinitions(Map<String, QueueGroupConfiguration> definitions) {
    queueGroups = copyGroupDefinitions(definitions);
    rebuildQueueGroups();
  }

  public void setQueueDefinitions(
    Map<String, QueueTypeConfiguration> typeDefinitions,
    Map<String, QueueGroupConfiguration> groupDefinitions
  ) {
    queueTypes = copyQueueDefinitions(typeDefinitions);
    queueGroups = copyGroupDefinitions(groupDefinitions);
    rebuildQueueTypes();
    rebuildQueueGroups();
  }

  public void setQueueServer(String queueServer) {
    this.queueServer = queueServer;
    rebuildKickWhenDownServers();
    rebuildQueueGroups();
  }

  public void setTargetServer(String targetServer) {
    this.targetServer = targetServer;
    rebuildKickWhenDownServers();
    rebuildQueueGroups();
  }

  public void setSourceServer(String sourceServer) {
    this.sourceServer = sourceServer;
    rebuildKickWhenDownServers();
    rebuildQueueGroups();
  }

  public void setEnableSourceServer(boolean enableSourceServer) {
    this.enableSourceServer = enableSourceServer;
    rebuildQueueGroups();
  }

  public void setAlwaysQueue(boolean alwaysQueue) {
    this.alwaysQueue = alwaysQueue;
  }

  public void setForceTargetServer(boolean forceTargetServer) {
    this.forceTargetServer = forceTargetServer;
  }

  public void setKickWhenDown(boolean kickWhenDown) {
    this.kickWhenDown = kickWhenDown;
  }

  public void setPauseQueueIfTargetDown(boolean pauseQueueIfTargetDown) {
    this.pauseQueueIfTargetDown = pauseQueueIfTargetDown;
  }

  public void setKickWhenDownServers(List<String> servers) {
    this.rawKickWhenDownServers = new ArrayList<>(servers);
    rebuildKickWhenDownServers();
  }

  public void setServerIsFullMessage(String message) {
    this.serverIsFullMessage = message;
  }

  public void setServerDownKickMessage(String message) {
    this.serverDownKickMessage = message;
  }

  public void setQueueBypassPermission(String permission) {
    this.queueBypassPermission = permission;
  }

  public void setIfTargetDownSendToQueue(boolean value) {
    this.ifTargetDownSendToQueue = value;
  }

  public void setIfTargetDownSendToQueueMessage(String message) {
    this.ifTargetDownSendToQueueMessage = message;
  }

  public void setDownWordList(List<String> words) {
    this.downWordList = new ArrayList<>(words);
  }

  public void setEnableKickMessage(boolean value) {
    this.enableKickMessage = value;
  }

  public void setKickMessage(String message) {
    this.kickMessage = message;
  }

  public void setRecoveryMessage(String message) {
    this.recoveryMessage = message;
  }

  public void setShadowBanType(BanType type) {
    this.shadowBanType = type;
  }

  public void setShadowBanMessage(String message) {
    this.shadowBanMessage = message;
  }

  public void setShadowBanPercent(int percent) {
    this.shadowBanPercent = percent;
  }

  public void setMaxPlayersPerMove(int maxPlayersPerMove) {
    this.maxPlayersPerMove = maxPlayersPerMove;
  }

  public void setJoiningTargetServer(String message) {
    this.joiningTargetServer = message;
  }

  public void setSendXpSound(boolean sendXpSound) {
    this.sendXpSound = sendXpSound;
  }

  public void setEnableUsernameRegex(boolean enable) {
    this.enableUsernameRegex = enable;
  }

  public void setUsernameRegex(String regex) {
    this.usernameRegex = regex;
  }

  public void setUsernameRegexMessage(String message) {
    this.usernameRegexMessage = message;
  }

  private void rebuildKickWhenDownServers() {
    List<String> resolved = new ArrayList<>();
    for (String text : rawKickWhenDownServers) {
      resolved.add(text
        .replace("%TARGET_SERVER%", targetServer)
        .replace("%QUEUE_SERVER%", queueServer)
        .replace("%SOURCE_SERVER%", sourceServer));
    }
    if (!resolved.contains(targetServer)) {
      resolved.add(targetServer);
    }
    kickWhenDownServers = resolved;
  }

  private void rebuildQueueTypes() {
    Map<String, QueueTypeConfiguration> definitions = queueTypes;
    queueTypeInstances.keySet().removeIf(name -> !definitions.containsKey(name));

    List<Map.Entry<String, QueueTypeConfiguration>> sorted = definitions.entrySet().stream()
      .sorted(Comparator.comparingInt(entry -> entry.getValue().getOrder()))
      .toList();

    Map<String, QueueType> rebuilt = new LinkedHashMap<>();
    for (Map.Entry<String, QueueTypeConfiguration> entry : sorted) {
      String name = entry.getKey();
      QueueTypeConfiguration configuration = entry.getValue();

      QueueType queueType = queueTypeInstances.get(name);
      if (queueType == null) {
        queueType = new QueueType(
          name,
          configuration.getOrder(),
          configuration.getPermission(),
          configuration.getSlots(),
          new ArrayList<>(configuration.getHeader()),
          new ArrayList<>(configuration.getFooter())
        );
      } else {
        queueType.setOrder(configuration.getOrder());
        queueType.setPermission(configuration.getPermission());
        queueType.setReservedSlots(configuration.getSlots());
        queueType.setHeader(new ArrayList<>(configuration.getHeader()));
        queueType.setFooter(new ArrayList<>(configuration.getFooter()));
      }
      rebuilt.put(name, queueType);
    }

    queueTypeInstances.clear();
    queueTypeInstances.putAll(rebuilt);
    orderedQueueTypes.clear();
    orderedQueueTypes.addAll(queueTypeInstances.values());
    orderedQueueTypes.sort(Comparator.comparingInt(QueueType::getOrder));
  }

  private void rebuildQueueGroups() {
    queueGroupList.clear();
    queueGroupsByName.clear();
    queueGroupsByTarget.clear();
    queueGroupByType.clear();
    defaultQueueGroup = null;

    if (queueGroups.isEmpty()) {
      queueGroups = defaultQueueGroups();
    }

    QueueGroup fallback = null;
    for (Map.Entry<String, QueueGroupConfiguration> entry : queueGroups.entrySet()) {
      String name = entry.getKey();
      QueueGroupConfiguration configuration = entry.getValue();

      String resolvedQueueServer = configuration.getQueueServer();
      if (resolvedQueueServer == null || resolvedQueueServer.isBlank()) {
        resolvedQueueServer = queueServer;
      }

      List<String> targetServers = configuration.getTargetServers();
      if (targetServers.isEmpty()) {
        targetServers = List.of(targetServer);
      }

      List<String> sourceServers = enableSourceServer ? configuration.getSourceServers() : List.of();
      if (enableSourceServer && sourceServers.isEmpty() && sourceServer != null && !sourceServer.isBlank()) {
        sourceServers = List.of(sourceServer);
      }

      List<String> typeNames = configuration.getQueueTypes();
      QueueType[] groupTypes;
      if (typeNames.isEmpty()) {
        groupTypes = queueTypeInstances.values().toArray(QueueType[]::new);
      } else {
        groupTypes = typeNames.stream()
          .map(nameKey -> {
            QueueType type = queueTypeInstances.get(nameKey);
            if (type == null) {
              throw new IllegalStateException("Queue group \"" + name + "\" references unknown queue type \"" + nameKey + "\"");
            }
            return type;
          })
          .toArray(QueueType[]::new);
      }

      QueueGroup group = new QueueGroup(
        name,
        resolvedQueueServer,
        targetServers,
        sourceServers,
        groupTypes
      );

      registerGroup(group);
      if (fallback == null) {
        fallback = group;
      }
      if (configuration.isDefaultGroup()) {
        defaultQueueGroup = group;
      }
    }

    if (defaultQueueGroup == null) {
      defaultQueueGroup = fallback;
    }

    orderedQueueTypes.removeIf(type -> !queueGroupByType.containsKey(type));
    orderedQueueTypes.sort(Comparator.comparingInt(QueueType::getOrder));
  }

  private void registerGroup(QueueGroup group) {
    queueGroupList.add(group);
    queueGroupsByName.put(group.getName().toLowerCase(Locale.ROOT), group);
    for (String target : group.getTargetServers()) {
      queueGroupsByTarget.put(target.toLowerCase(Locale.ROOT), group);
    }
    for (QueueType type : group.getQueueTypes()) {
      queueGroupByType.put(type, group);
    }
  }

  private Map<String, QueueTypeConfiguration> defaultQueueTypes() {
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

  private Map<String, QueueGroupConfiguration> defaultQueueGroups() {
    Map<String, QueueGroupConfiguration> defaults = new LinkedHashMap<>();
    QueueGroupConfiguration configuration = new QueueGroupConfiguration();
    configuration.queueServer = queueServer;
    configuration.targetServers = new ArrayList<>(List.of(targetServer));
    configuration.sourceServers = new ArrayList<>(List.of(sourceServer));
    configuration.queueTypes = new ArrayList<>(queueTypes.keySet());
    configuration.defaultGroup = true;
    defaults.put("default", configuration);
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
    configuration.order = order;
    configuration.slots = slots;
    configuration.permission = permission;
    configuration.header = header;
    configuration.footer = footer;
    return configuration;
  }

  private static Map<String, QueueTypeConfiguration> copyQueueDefinitions(Map<String, QueueTypeConfiguration> original) {
    Map<String, QueueTypeConfiguration> copy = new LinkedHashMap<>();
    original.forEach((name, definition) -> copy.put(name, definition.copy()));
    return copy;
  }

  private static Map<String, QueueGroupConfiguration> copyGroupDefinitions(Map<String, QueueGroupConfiguration> original) {
    Map<String, QueueGroupConfiguration> copy = new LinkedHashMap<>();
    original.forEach((name, definition) -> copy.put(name, definition.copy()));
    return copy;
  }

  public static Config fromLegacy(ConfigLegacyV1 legacy) {
    Config config = new Config();
    config.configVersion = CURRENT_VERSION;
    config.serverName = legacy.SERVER_NAME;
    config.enableUsernameRegex = legacy.ENABLE_USERNAME_REGEX;
    config.usernameRegex = legacy.USERNAME_REGEX;
    config.usernameRegexMessage = legacy.USERNAME_REGEX_MESSAGE;
    config.registerTab = legacy.REGISTER_TAB;
    config.serverIsFullMessage = legacy.SERVER_IS_FULL_MESSAGE;
    config.serverOnlineCheckDelay = legacy.SERVER_ONLINE_CHECK_DELAY;
    config.positionMessageChat = legacy.POSITION_MESSAGE_CHAT;
    config.positionMessageHotBar = legacy.POSITION_MESSAGE_HOT_BAR;
    config.queuePosition = legacy.QUEUE_POSITION;
    config.positionMessageDelay = legacy.POSITION_MESSAGE_DELAY;
    config.positionPlayerList = legacy.POSITION_PLAYER_LIST;
    config.enableKickMessage = legacy.ENABLE_KICK_MESSAGE;
    config.kickMessage = legacy.KICK_MESSAGE;
    config.pauseQueueIfTargetDown = legacy.PAUSE_QUEUE_IF_TARGET_DOWN;
    config.pauseQueueIfTargetDownMessage = legacy.PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE;
    config.kickWhenDown = legacy.KICK_WHEN_DOWN;
    config.serverDownKickMessage = legacy.SERVER_DOWN_KICK_MESSAGE;
    config.rawKickWhenDownServers = new ArrayList<>(legacy.RAW_KICK_WHEN_DOWN_SERVERS);
    config.ifTargetDownSendToQueue = legacy.IF_TARGET_DOWN_SEND_TO_QUEUE;
    config.ifTargetDownSendToQueueMessage = legacy.IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE;
    config.downWordList = new ArrayList<>(legacy.DOWN_WORD_LIST);
    config.recovery = legacy.RECOVERY;
    config.recoveryMessage = legacy.RECOVERY_MESSAGE;
    config.queueServer = legacy.QUEUE_SERVER;
    config.targetServer = legacy.TARGET_SERVER;
    config.forceTargetServer = legacy.FORCE_TARGET_SERVER;
    config.enableSourceServer = legacy.ENABLE_SOURCE_SERVER;
    config.sourceServer = legacy.SOURCE_SERVER;
    config.joiningTargetServer = legacy.JOINING_TARGET_SERVER;
    config.queueServerSlots = legacy.QUEUE_SERVER_SLOTS;
    config.queueMoveDelay = legacy.QUEUE_MOVE_DELAY;
    config.maxPlayersPerMove = legacy.MAX_PLAYERS_PER_MOVE;
    config.alwaysQueue = legacy.ALWAYS_QUEUE;
    config.sendXpSound = legacy.SEND_XP_SOUND;
    config.shadowBanType = legacy.SHADOW_BAN_TYPE;
    config.shadowBanPercent = legacy.PERCENT;
    config.shadowBanMessage = legacy.SHADOW_BAN_MESSAGE;
    config.queueBypassPermission = legacy.QUEUE_BYPASS_PERMISSION;
    config.adminPermission = legacy.ADMIN_PERMISSION;
    config.queueTypes = copyQueueDefinitions(legacy.QUEUE_TYPE_DEFINITIONS);
    config.queueGroups = config.defaultQueueGroups();
    config.postProcess();
    return config;
  }

  @Configuration
  public static final class QueueTypeConfiguration {
    private int order = 1;
    private int slots = 0;
    private String permission = "default";
    private List<String> header = new ArrayList<>();
    private List<String> footer = new ArrayList<>();

    public int getOrder() {
      return order;
    }

    public int getSlots() {
      return slots;
    }

    public String getPermission() {
      return permission;
    }

    public List<String> getHeader() {
      return new ArrayList<>(header);
    }

    public List<String> getFooter() {
      return new ArrayList<>(footer);
    }

    public void setOrder(int order) {
      this.order = order;
    }

    public void setSlots(int slots) {
      this.slots = slots;
    }

    public void setPermission(String permission) {
      this.permission = permission;
    }

    public void setHeader(List<String> header) {
      this.header = new ArrayList<>(header);
    }

    public void setFooter(List<String> footer) {
      this.footer = new ArrayList<>(footer);
    }

    private QueueTypeConfiguration copy() {
      QueueTypeConfiguration configuration = new QueueTypeConfiguration();
      configuration.order = order;
      configuration.slots = slots;
      configuration.permission = permission;
      configuration.header = new ArrayList<>(header);
      configuration.footer = new ArrayList<>(footer);
      return configuration;
    }
  }

  @Configuration
  public static final class QueueGroupConfiguration {
    private String queueServer = "";
    private List<String> targetServers = new ArrayList<>();
    private List<String> sourceServers = new ArrayList<>();
    private List<String> queueTypes = new ArrayList<>();
    private boolean defaultGroup = false;

    public String getQueueServer() {
      return queueServer;
    }

    public List<String> getTargetServers() {
      return new ArrayList<>(targetServers);
    }

    public List<String> getSourceServers() {
      return new ArrayList<>(sourceServers);
    }

    public List<String> getQueueTypes() {
      return new ArrayList<>(queueTypes);
    }

    public boolean isDefaultGroup() {
      return defaultGroup;
    }

    public void setQueueServer(String queueServer) {
      this.queueServer = queueServer;
    }

    public void setTargetServers(List<String> targetServers) {
      this.targetServers = new ArrayList<>(targetServers);
    }

    public void setSourceServers(List<String> sourceServers) {
      this.sourceServers = new ArrayList<>(sourceServers);
    }

    public void setQueueTypes(List<String> queueTypes) {
      this.queueTypes = new ArrayList<>(queueTypes);
    }

    public void setDefaultGroup(boolean defaultGroup) {
      this.defaultGroup = defaultGroup;
    }

    private QueueGroupConfiguration copy() {
      QueueGroupConfiguration configuration = new QueueGroupConfiguration();
      configuration.queueServer = queueServer;
      configuration.targetServers = new ArrayList<>(targetServers);
      configuration.sourceServers = new ArrayList<>(sourceServers);
      configuration.queueTypes = new ArrayList<>(queueTypes);
      configuration.defaultGroup = defaultGroup;
      return configuration;
    }
  }

  @Configuration
  public static final class ConfigLegacyV1 {
    public String SERVER_NAME = "&cexample.org";
    public boolean ENABLE_USERNAME_REGEX = true;
    public String USERNAME_REGEX = "[a-zA-Z0-9_]*";
    public String USERNAME_REGEX_MESSAGE = "&6[PQ] Invalid username please use: %regex%";
    public boolean REGISTER_TAB = true;
    public String SERVER_IS_FULL_MESSAGE = "%server_name% &6is full";
    public int SERVER_ONLINE_CHECK_DELAY = 500;
    public boolean POSITION_MESSAGE_CHAT = true;
    public boolean POSITION_MESSAGE_HOT_BAR = false;
    public String QUEUE_POSITION = "&6Position in queue: &l%position%";
    public int POSITION_MESSAGE_DELAY = 10000;
    public boolean POSITION_PLAYER_LIST = true;
    public boolean ENABLE_KICK_MESSAGE = false;
    public String KICK_MESSAGE = "&6You have lost connection to the server";
    public boolean PAUSE_QUEUE_IF_TARGET_DOWN = true;
    public String PAUSE_QUEUE_IF_TARGET_DOWN_MESSAGE = "&6The main server is down. We will be back soon!";
    public boolean KICK_WHEN_DOWN = false;
    public String SERVER_DOWN_KICK_MESSAGE = "%server_name% &6is down please try again later :(";
    public List<String> RAW_KICK_WHEN_DOWN_SERVERS = new ArrayList<>(List.of(
      "%TARGET_SERVER%",
      "%QUEUE_SERVER%"
    ));
    public boolean IF_TARGET_DOWN_SEND_TO_QUEUE = true;
    public String IF_TARGET_DOWN_SEND_TO_QUEUE_MESSAGE = "&cThe target server is offline now! You have been sent to queue while it goes back online.";
    public List<String> DOWN_WORD_LIST = new ArrayList<>(List.of(
      "restarting",
      "closed",
      "went down",
      "unknown reason"
    ));
    public boolean RECOVERY = true;
    public String RECOVERY_MESSAGE = "&cOops something went wrong... Putting you back in queue.";
    public String QUEUE_SERVER = "queue";
    public String TARGET_SERVER = "main";
    public boolean FORCE_TARGET_SERVER = false;
    public boolean ENABLE_SOURCE_SERVER = false;
    public String SOURCE_SERVER = "lobby";
    public String JOINING_TARGET_SERVER = "&6Connecting to the server...";
    public int QUEUE_SERVER_SLOTS = 9000;
    public int QUEUE_MOVE_DELAY = 1000;
    public int MAX_PLAYERS_PER_MOVE = 10;
    public boolean ALWAYS_QUEUE = false;
    public boolean SEND_XP_SOUND = true;
    public BanType SHADOW_BAN_TYPE = BanType.LOOP;
    public int PERCENT = 10;
    public String SHADOW_BAN_MESSAGE = "&6You have lost connection to the server";
    public String QUEUE_BYPASS_PERMISSION = "queue.bypass";
    public String ADMIN_PERMISSION = "queue.admin";
    public Map<String, QueueTypeConfiguration> QUEUE_TYPE_DEFINITIONS = defaultQueueTypesLegacy();

    private static Map<String, QueueTypeConfiguration> defaultQueueTypesLegacy() {
      Config config = new Config();
      return copyQueueDefinitions(config.defaultQueueTypes());
    }
  }
}
