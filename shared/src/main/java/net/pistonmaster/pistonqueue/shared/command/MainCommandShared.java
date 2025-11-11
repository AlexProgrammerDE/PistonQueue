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
package net.pistonmaster.pistonqueue.shared.command;

import net.pistonmaster.pistonqueue.shared.chat.ComponentWrapperFactory;
import net.pistonmaster.pistonqueue.shared.chat.TextColorWrapper;
import net.pistonmaster.pistonqueue.shared.chat.TextDecorationWrapper;
import net.pistonmaster.pistonqueue.shared.config.Config;
import net.pistonmaster.pistonqueue.shared.plugin.PistonQueuePlugin;
import net.pistonmaster.pistonqueue.shared.queue.QueueGroup;
import net.pistonmaster.pistonqueue.shared.queue.QueueType;
import net.pistonmaster.pistonqueue.shared.utils.StorageTool;
import net.pistonmaster.pistonqueue.shared.wrapper.CommandSourceWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.PermissibleWrapper;
import net.pistonmaster.pistonqueue.shared.wrapper.PlayerWrapper;

import java.util.*;
import java.util.concurrent.locks.Lock;

public interface MainCommandShared {
  List<String> commands = List.of("help", "version", "stats");
  List<String> adminCommands = List.of("slotstats", "reload", "shadowban", "unshadowban");

  default void onCommand(CommandSourceWrapper sender, String[] args, PistonQueuePlugin plugin) {
    Config config = plugin.getConfiguration();
    if (args.length == 0) {
      help(sender, config);
      return;
    }

    switch (args[0].toLowerCase(Locale.ROOT)) {
      case "version" -> {
        sendLine(sender);
        sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(component().text("Version " + plugin.getVersion() + " by").color(TextColorWrapper.GOLD));
        sender.sendMessage(component().text(String.join(", ", plugin.getAuthors())).color(TextColorWrapper.GOLD));
        sendLine(sender);
      }
      case "stats" -> {
        sendLine(sender);
        sender.sendMessage(component().text("Queue stats").color(TextColorWrapper.GOLD));
        for (QueueGroup group : config.getQueueGroups()) {
          sender.sendMessage(component().text(group.getName()).color(TextColorWrapper.GOLD));
          for (QueueType type : group.getQueueTypes()) {
            sender.sendMessage(component().text(" - " + type.getName() + ": ").color(TextColorWrapper.GOLD)
              .append(component().text(String.valueOf(queueSize(type))).color(TextColorWrapper.GOLD).decorate(TextDecorationWrapper.BOLD)));
          }
        }
        sendLine(sender);
      }
      case "slotstats" -> {
        if (!sender.hasPermission(config.adminPermission())) {
          noPermission(sender);
          return;
        }

        sendLine(sender);
        sender.sendMessage(component().text("Target slot stats").color(TextColorWrapper.GOLD));
        for (QueueGroup group : config.getQueueGroups()) {
          sender.sendMessage(component().text(group.getName()).color(TextColorWrapper.GOLD));
          for (QueueType type : group.getQueueTypes()) {
            sender.sendMessage(component().text(" - " + type.getName() + ": ").color(TextColorWrapper.GOLD)
              .append(component().text(type.getPlayersWithTypeInTarget().get() + " / " + type.getReservedSlots()).color(TextColorWrapper.GOLD).decorate(TextDecorationWrapper.BOLD)));
          }
        }
        sendLine(sender);
      }
      case "reload" -> {
        if (!sender.hasPermission(config.adminPermission())) {
          noPermission(sender);
          return;
        }

        plugin.processConfig(plugin.getDataDirectory());

        sendLine(sender);
        sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(component().text("Config reloaded").color(TextColorWrapper.GREEN));
        sendLine(sender);
      }
      case "shadowban" -> {
        if (!sender.hasPermission(config.adminPermission())) {
          noPermission(sender);
          return;
        }

        if (args.length == 1) {
          sendBanHelp(sender);
          return;
        }

        if (args.length == 2) {
          sendBanHelp(sender);
          return;
        }

        Calendar calendar = Calendar.getInstance();
        if (args[2].toLowerCase(Locale.ROOT).endsWith("d")) {
          int d = Integer.parseInt(args[2].toLowerCase(Locale.ROOT).replace("d", ""));

          calendar.add(Calendar.DAY_OF_WEEK, d);
        } else if (args[2].toLowerCase(Locale.ROOT).endsWith("h")) {
          int h = Integer.parseInt(args[2].toLowerCase(Locale.ROOT).replace("h", ""));

          calendar.add(Calendar.HOUR_OF_DAY, h);
        } else if (args[2].toLowerCase(Locale.ROOT).endsWith("m")) {
          int m = Integer.parseInt(args[2].toLowerCase(Locale.ROOT).replace("m", ""));

          calendar.add(Calendar.MINUTE, m);
        } else if (args[2].toLowerCase(Locale.ROOT).endsWith("s")) {
          int s = Integer.parseInt(args[2].toLowerCase(Locale.ROOT).replace("s", ""));

          calendar.add(Calendar.SECOND, s);
        } else {
          sendBanHelp(sender);
          return;
        }

        String banPlayerName = args[1];
        if (StorageTool.shadowBanPlayer(banPlayerName, calendar.getTime())) {
          sendLine(sender);
          sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
          sender.sendMessage(component().text("Successfully shadowbanned " + banPlayerName + "!").color(TextColorWrapper.GREEN));
          sendLine(sender);
        } else {
          sendLine(sender);
          sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
          sender.sendMessage(component().text(banPlayerName + " is already shadowbanned!").color(TextColorWrapper.RED));
          sendLine(sender);
        }

      }
      case "unshadowban" -> {
        if (!sender.hasPermission(config.adminPermission())) {
          noPermission(sender);
          return;
        }

        if (args.length == 1) {
          sendUnBanHelp(sender);
          return;
        }

        String unBanPlayerName = args[1];
        if (StorageTool.unShadowBanPlayer(unBanPlayerName)) {
          sendLine(sender);
          sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
          sender.sendMessage(component().text("Successfully unshadowbanned " + unBanPlayerName + "!").color(TextColorWrapper.GREEN));
          sendLine(sender);
        } else {
          sendLine(sender);
          sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
          sender.sendMessage(component().text(unBanPlayerName + " is not shadowbanned!").color(TextColorWrapper.RED));
          sendLine(sender);
        }

      }
      default -> help(sender, config);
    }
  }

  default void noPermission(CommandSourceWrapper sender) {
    sendLine(sender);
    sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("You do not").color(TextColorWrapper.RED));
    sender.sendMessage(component().text("have permission").color(TextColorWrapper.RED));
    sendLine(sender);
  }

  default void help(CommandSourceWrapper sender, Config config) {
    sendLine(sender);
    sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("/pq help").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("/pq version").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("/pq stats").color(TextColorWrapper.GOLD));

    if (sender.hasPermission(config.adminPermission())) {
      sender.sendMessage(component().text("/pq slotstats").color(TextColorWrapper.GOLD));
      sender.sendMessage(component().text("/pq reload").color(TextColorWrapper.GOLD));
      sender.sendMessage(component().text("/pq shadowban").color(TextColorWrapper.GOLD));
      sender.sendMessage(component().text("/pq unshadowban").color(TextColorWrapper.GOLD));
    }

    sendLine(sender);
  }

  default void sendBanHelp(CommandSourceWrapper sender) {
    sendLine(sender);
    sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("/pq shadowban player <d|h|m|s>").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("Example:").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("/pq shadowban Pistonmaster 2d").color(TextColorWrapper.GOLD));
    sendLine(sender);
  }

  default void sendUnBanHelp(CommandSourceWrapper sender) {
    sendLine(sender);
    sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("/pq unshadowban player").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("Example:").color(TextColorWrapper.GOLD));
    sender.sendMessage(component().text("/pq unshadowban Pistonmaster").color(TextColorWrapper.GOLD));
    sendLine(sender);
  }

  default void sendLine(CommandSourceWrapper sender) {
    sender.sendMessage(component().text("----------------").color(TextColorWrapper.DARK_BLUE));
  }

  default List<String> onTab(String[] args, PermissibleWrapper wrapper, PistonQueuePlugin plugin) {
    Config config = plugin.getConfiguration();
    if (config.registerTab()) {
      final List<String> completions = new ArrayList<>();

      if (args.length == 1) {
        for (String string : commands) {
          if (string.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
            completions.add(string);
          }
        }

        if (wrapper.hasPermission(config.adminPermission())) {
          for (String string : adminCommands) {
            if (string.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
              completions.add(string);
            }
          }
        }
      } else if (wrapper.hasPermission(config.adminPermission())
        && args.length == 2
        && ("shadowban".equalsIgnoreCase(args[0]) || "unshadowban".equalsIgnoreCase(args[0]))) {
        addPlayers(completions, args, plugin);
      }

      Collections.sort(completions);

      return List.copyOf(completions);
    } else {
      return List.of();
    }
  }

  default void addPlayers(List<String> completions, String[] args, PistonQueuePlugin proxy) {
    for (PlayerWrapper player : proxy.getPlayers()) {
      if (player.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
        completions.add(player.getName());
      }
    }
  }

  private static int queueSize(QueueType type) {
    Lock readLock = type.getQueueLock().readLock();
    readLock.lock();
    try {
      return type.getQueueMap().size();
    } finally {
      readLock.unlock();
    }
  }

  ComponentWrapperFactory component();
}
