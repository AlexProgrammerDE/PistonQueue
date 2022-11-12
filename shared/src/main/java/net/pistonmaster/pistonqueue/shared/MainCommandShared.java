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
        if (args.length == 0)
            help(sender);

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "version":
                    sendLine(sender);
                    sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                    sender.sendMessage(getWrapperFactory().text("Version " + plugin.getVersion() + " by").color(TextColorWrapper.GOLD));
                    sender.sendMessage(getWrapperFactory().text(String.join(", ", plugin.getAuthors())).color(TextColorWrapper.GOLD));
                    sendLine(sender);
                    break;
                case "stats":
                    sendLine(sender);
                    sender.sendMessage(getWrapperFactory().text("Queue stats").color(TextColorWrapper.GOLD));
                    for (QueueType type : Config.QUEUE_TYPES) {
                        sender.sendMessage(getWrapperFactory().text(type.getName() + ": ").color(TextColorWrapper.GOLD).append(getWrapperFactory().text(String.valueOf(type.getQueueMap().size())).color(TextColorWrapper.GOLD).decorate(TextDecorationWrapper.BOLD)));
                    }
                    sendLine(sender);
                    break;
                case "slotstats":
                    if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                        sendLine(sender);
                        sender.sendMessage(getWrapperFactory().text("Target slot stats").color(TextColorWrapper.GOLD));
                        for (QueueType type : Config.QUEUE_TYPES) {
                            sender.sendMessage(getWrapperFactory().text(type.getName() + ": ").color(TextColorWrapper.GOLD).append(getWrapperFactory().text(type.getPlayersWithTypeInTarget().get() + " / " + type.getReservedSlots()).color(TextColorWrapper.GOLD).decorate(TextDecorationWrapper.BOLD)));
                        }
                        sendLine(sender);
                    } else {
                        noPermission(sender);
                    }
                    break;
                case "reload":
                    if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                        plugin.processConfig(plugin.getDataDirectory());

                        sendLine(sender);
                        sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                        sender.sendMessage(getWrapperFactory().text("Config reloaded").color(TextColorWrapper.GREEN));
                        sendLine(sender);
                    } else {
                        noPermission(sender);
                    }
                    break;
                case "shadowban":
                    if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                        if (args.length > 1) {
                            Optional<PlayerWrapper> optionalPlayer = plugin.getPlayer(args[1]);
                            if (optionalPlayer.isPresent()) {
                                PlayerWrapper player = optionalPlayer.get();

                                if (args.length > 2) {
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
                                        break;
                                    }

                                    if (StorageTool.shadowBanPlayer(player.getUniqueId(), calendar.getTime())) {
                                        sendLine(sender);
                                        sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                                        sender.sendMessage(getWrapperFactory().text("Successfully shadowbanned " + player.getName() + "!").color(TextColorWrapper.GREEN));
                                        sendLine(sender);
                                    } else {
                                        sendLine(sender);
                                        sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                                        sender.sendMessage(getWrapperFactory().text(player.getName() + " is already shadowbanned!").color(TextColorWrapper.RED));
                                        sendLine(sender);
                                    }
                                } else {
                                    sendBanHelp(sender);
                                }
                            } else {
                                sendLine(sender);
                                sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                                sender.sendMessage(getWrapperFactory().text("The player " + args[1] + " was not found!").color(TextColorWrapper.GOLD));
                                sendLine(sender);
                            }
                        } else {
                            sendBanHelp(sender);
                        }
                    } else {
                        noPermission(sender);
                    }
                    break;
                case "unshadowban":
                    if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                        if (args.length > 1) {
                            Optional<PlayerWrapper> optionalPlayer = plugin.getPlayer(args[1]);
                            if (optionalPlayer.isPresent()) {
                                PlayerWrapper player = optionalPlayer.get();

                                if (StorageTool.unShadowBanPlayer(player.getUniqueId())) {
                                    sendLine(sender);
                                    sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                                    sender.sendMessage(getWrapperFactory().text("Successfully unshadowbanned " + player.getName() + "!").color(TextColorWrapper.GREEN));
                                    sendLine(sender);
                                } else {
                                    sendLine(sender);
                                    sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                                    sender.sendMessage(getWrapperFactory().text(player.getName() + " is already shadowbanned!").color(TextColorWrapper.RED));
                                    sendLine(sender);
                                }
                            } else {
                                sendLine(sender);
                                sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
                                sender.sendMessage(getWrapperFactory().text("The player " + args[1] + " was not found!").color(TextColorWrapper.GOLD));
                                sendLine(sender);
                            }
                        } else {
                            sendUnBanHelp(sender);
                        }
                    } else {
                        noPermission(sender);
                    }
                    break;
                default:
                    help(sender);
                    break;
            }
        }
    }

    default void noPermission(CommandSourceWrapper sender) {
        sendLine(sender);
        sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("You do not").color(TextColorWrapper.RED));
        sender.sendMessage(getWrapperFactory().text("have permission").color(TextColorWrapper.RED));
        sendLine(sender);
    }

    default void help(CommandSourceWrapper sender) {
        sendLine(sender);
        sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("/pq help").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("/pq version").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("/pq stats").color(TextColorWrapper.GOLD));

        if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
            sender.sendMessage(getWrapperFactory().text("/pq slotstats").color(TextColorWrapper.GOLD));
            sender.sendMessage(getWrapperFactory().text("/pq reload").color(TextColorWrapper.GOLD));
            sender.sendMessage(getWrapperFactory().text("/pq shadowban").color(TextColorWrapper.GOLD));
            sender.sendMessage(getWrapperFactory().text("/pq unshadowban").color(TextColorWrapper.GOLD));
        }

        sendLine(sender);
    }

    default void sendBanHelp(CommandSourceWrapper sender) {
        sendLine(sender);
        sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("/pq shadowban player <d|h|m|s>").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("Example:").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("/pq shadowban Pistonmaster 2d").color(TextColorWrapper.GOLD));
        sendLine(sender);
    }

    default void sendUnBanHelp(CommandSourceWrapper sender) {
        sendLine(sender);
        sender.sendMessage(getWrapperFactory().text("PistonQueue").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("/pq unshadowban player").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("Example:").color(TextColorWrapper.GOLD));
        sender.sendMessage(getWrapperFactory().text("/pq unshadowban Pistonmaster").color(TextColorWrapper.GOLD));
        sendLine(sender);
    }

    default void sendLine(CommandSourceWrapper sender) {
        sender.sendMessage(getWrapperFactory().text("----------------").color(TextColorWrapper.DARK_BLUE));
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

    ComponentWrapperFactory getWrapperFactory();
}
