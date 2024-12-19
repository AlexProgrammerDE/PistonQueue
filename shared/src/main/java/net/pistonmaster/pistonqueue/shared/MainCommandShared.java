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
package net.pistonmaster.pistonqueue.shared;

import java.util.*;

public interface MainCommandShared {
    String[] commands = {"help", "version", "stats"};
    String[] adminCommands = {"slotstats", "reload", "shadowban", "unshadowban"};

    default void onCommand(CommandSourceWrapper sender, String[] args, PistonQueuePlugin plugin) {
        if (args.length == 0) {
            help(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "version":
                sendLine(sender);
                sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
                sender.sendMessage(component().text("Version " + plugin.getVersion() + " by").color(TextColorWrapper.GOLD));
                sender.sendMessage(component().text(String.join(", ", plugin.getAuthors())).color(TextColorWrapper.GOLD));
                sendLine(sender);
                return;
            case "stats":
                sendLine(sender);
                sender.sendMessage(component().text("Queue stats").color(TextColorWrapper.GOLD));
                for (QueueType type : Config.QUEUE_TYPES) {
                    sender.sendMessage(component().text(type.getName() + ": ").color(TextColorWrapper.GOLD)
                            .append(component().text(String.valueOf(type.getQueueMap().size())).color(TextColorWrapper.GOLD).decorate(TextDecorationWrapper.BOLD)));
                }
                sendLine(sender);
                return;
            case "slotstats":
                if (!sender.hasPermission(Config.ADMIN_PERMISSION)) {
                    noPermission(sender);
                    return;
                }

                sendLine(sender);
                sender.sendMessage(component().text("Target slot stats").color(TextColorWrapper.GOLD));
                for (QueueType type : Config.QUEUE_TYPES) {
                    sender.sendMessage(component().text(type.getName() + ": ").color(TextColorWrapper.GOLD).append(component().text(type.getPlayersWithTypeInTarget().get() + " / " + type.getReservedSlots()).color(TextColorWrapper.GOLD).decorate(TextDecorationWrapper.BOLD)));
                }
                sendLine(sender);
                return;
            case "reload":
                if (!sender.hasPermission(Config.ADMIN_PERMISSION)) {
                    noPermission(sender);
                    return;
                }

                plugin.processConfig(plugin.getDataDirectory());

                sendLine(sender);
                sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
                sender.sendMessage(component().text("Config reloaded").color(TextColorWrapper.GREEN));
                sendLine(sender);
                return;
            case "shadowban":
                if (!sender.hasPermission(Config.ADMIN_PERMISSION)) {
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
                calendar.setTime(new Date());

                if (args[2].toLowerCase().endsWith("d")) {
                    int d = Integer.parseInt(args[2].toLowerCase().replace("d", ""));

                    calendar.add(Calendar.DAY_OF_WEEK, d);
                } else if (args[2].toLowerCase().endsWith("h")) {
                    int h = Integer.parseInt(args[2].toLowerCase().replace("h", ""));

                    calendar.add(Calendar.HOUR_OF_DAY, h);
                } else if (args[2].toLowerCase().endsWith("m")) {
                    int m = Integer.parseInt(args[2].toLowerCase().replace("m", ""));

                    calendar.add(Calendar.MINUTE, m);
                } else if (args[2].toLowerCase().endsWith("s")) {
                    int s = Integer.parseInt(args[2].toLowerCase().replace("s", ""));

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

                return;
            case "unshadowban":
                if (!sender.hasPermission(Config.ADMIN_PERMISSION)) {
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

                return;
            default: {
                help(sender);
            }
        }
    }

    default void noPermission(CommandSourceWrapper sender) {
        sendLine(sender);
        sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(component().text("You do not").color(TextColorWrapper.RED));
        sender.sendMessage(component().text("have permission").color(TextColorWrapper.RED));
        sendLine(sender);
    }

    default void help(CommandSourceWrapper sender) {
        sendLine(sender);
        sender.sendMessage(component().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(component().text("/pq help").color(TextColorWrapper.GOLD));
        sender.sendMessage(component().text("/pq version").color(TextColorWrapper.GOLD));
        sender.sendMessage(component().text("/pq stats").color(TextColorWrapper.GOLD));

        if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
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
        if (Config.REGISTER_TAB) {
            final List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                for (String string : commands) {
                    if (string.toLowerCase().startsWith(args[0].toLowerCase()))
                        completions.add(string);
                }

                if (wrapper.hasPermission(Config.ADMIN_PERMISSION)) {
                    for (String string : adminCommands) {
                        if (string.toLowerCase().startsWith(args[0].toLowerCase()))
                            completions.add(string);
                    }
                }
            } else if (wrapper.hasPermission(Config.ADMIN_PERMISSION)
                    && args.length == 2
                    && (args[0].equalsIgnoreCase("shadowban") || args[0].equalsIgnoreCase("unshadowban"))) {
                addPlayers(completions, args, plugin);
            }

            Collections.sort(completions);

            return completions;
        } else {
            return Collections.emptyList();
        }
    }

    default void addPlayers(List<String> completions, String[] args, PistonQueuePlugin proxy) {
        for (PlayerWrapper player : proxy.getPlayers()) {
            if (player.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                completions.add(player.getName());
        }
    }

    ComponentWrapperFactory component();
}
