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
package net.pistonmaster.pistonqueue.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.pistonmaster.pistonqueue.shared.Config;
import net.pistonmaster.pistonqueue.shared.QueueAPI;
import net.pistonmaster.pistonqueue.shared.QueueType;
import net.pistonmaster.pistonqueue.shared.StorageTool;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;

import java.util.*;

public final class MainCommand implements SimpleCommand {
    private static final String[] commands = {"help", "version", "stats"};
    private static final String[] adminCommands = {"slotstats", "reload", "shadowban", "unshadowban"};
    private final PistonQueueVelocity plugin;

    public MainCommand(PistonQueueVelocity plugin) {
        this.plugin = plugin;
    }

    private void noPermission(CommandSource sender) {
        sendLine(sender);
        sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("You do not").color(NamedTextColor.RED));
        sender.sendMessage(Component.text("have permission").color(NamedTextColor.RED));
        sendLine(sender);
    }

    private void help(CommandSource sender) {
        sendLine(sender);
        sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pq help").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pq version").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pq stats").color(NamedTextColor.GOLD));

        if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
            sender.sendMessage(Component.text("/pq slotstats").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/pq reload").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/pq shadowban").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("/pq unshadowban").color(NamedTextColor.GOLD));
        }

        sendLine(sender);
    }

    private void sendBanHelp(CommandSource sender) {
        sendLine(sender);
        sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pq shadowban player <d|h|m|s>").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Example:").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pq shadowban Pistonmaster 2d").color(NamedTextColor.GOLD));
        sendLine(sender);
    }

    private void sendUnBanHelp(CommandSource sender) {
        sendLine(sender);
        sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pq unshadowban player").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Example:").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pq unshadowban Pistonmaster").color(NamedTextColor.GOLD));
        sendLine(sender);
    }

    private void sendLine(CommandSource sender) {
        sender.sendMessage(Component.text("----------------").color(NamedTextColor.DARK_BLUE));
    }

    private void addPlayers(List<String> completions, String[] args) {
        for (Player player : plugin.getProxyServer().getAllPlayers()) {
            if (player.getUsername().toLowerCase().startsWith(args[1].toLowerCase()))
                completions.add(player.getUsername());
        }
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();

        if (args.length == 0)
            help(sender);

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "version":
                    sendLine(sender);
                    sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("Version " + plugin.getPluginContainer().getDescription().getVersion() + " by").color(NamedTextColor.GOLD));
                    sender.sendMessage(Component.text(String.join(", ", plugin.getPluginContainer().getDescription().getAuthors())).color(NamedTextColor.GOLD));
                    sendLine(sender);
                    break;
                case "stats":
                    sendLine(sender);
                    sender.sendMessage(Component.text("Queue stats").color(NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("Regular: ").color(NamedTextColor.GOLD).append(Component.text(String.valueOf(QueueAPI.getRegularSize())).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
                    sender.sendMessage(Component.text("Priority: ").color(NamedTextColor.GOLD).append(Component.text(String.valueOf(QueueAPI.getPrioritySize())).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
                    sender.sendMessage(Component.text("Veteran: ").color(NamedTextColor.GOLD).append(Component.text(String.valueOf(QueueAPI.getVeteranSize())).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
                    sendLine(sender);
                    break;
                case "slotstats":
                    if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                        sendLine(sender);
                        sender.sendMessage(Component.text("Main slot stats").color(NamedTextColor.GOLD));
                        sender.sendMessage(Component.text("Regular: ").color(NamedTextColor.GOLD).append(Component.text(QueueType.REGULAR.getPlayersWithTypeInMain().get() + "/" + QueueType.REGULAR.getReservedSlots()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
                        sender.sendMessage(Component.text("Priority: ").color(NamedTextColor.GOLD).append(Component.text(QueueType.PRIORITY.getPlayersWithTypeInMain().get() + "/" + QueueType.PRIORITY.getReservedSlots()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
                        sender.sendMessage(Component.text("Veteran: ").color(NamedTextColor.GOLD).append(Component.text(QueueType.VETERAN.getPlayersWithTypeInMain().get() + "/" + QueueType.VETERAN.getReservedSlots()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)));
                        sendLine(sender);
                    } else {
                        noPermission(sender);
                    }
                    break;
                case "reload":
                    if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                        plugin.processConfig(plugin.getDataDirectory());

                        sendLine(sender);
                        sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                        sender.sendMessage(Component.text("Config reloaded").color(NamedTextColor.GREEN));
                        sendLine(sender);
                    } else {
                        noPermission(sender);
                    }
                    break;
                case "shadowban":
                    if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                        if (args.length > 1) {
                            if (plugin.getProxyServer().getPlayer(args[1]).isPresent()) {
                                Player player = plugin.getProxyServer().getPlayer(args[1]).get();

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
                                        sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                                        sender.sendMessage(Component.text("Successfully shadowbanned " + player.getUsername() + "!").color(NamedTextColor.GREEN));
                                        sendLine(sender);
                                    } else {
                                        sendLine(sender);
                                        sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                                        sender.sendMessage(Component.text(player.getUsername() + " is already shadowbanned!").color(NamedTextColor.RED));
                                        sendLine(sender);
                                    }
                                } else {
                                    sendBanHelp(sender);
                                }
                            } else {
                                sendLine(sender);
                                sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                                sender.sendMessage(Component.text("The player " + args[1] + " was not found!").color(NamedTextColor.GOLD));
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
                            if (plugin.getProxyServer().getPlayer(args[1]).isPresent()) {
                                Player player = plugin.getProxyServer().getPlayer(args[1]).get();

                                if (StorageTool.unShadowBanPlayer(player.getUniqueId())) {
                                    sendLine(sender);
                                    sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text("Successfully unshadowbanned " + player.getUsername() + "!").color(NamedTextColor.GREEN));
                                    sendLine(sender);
                                } else {
                                    sendLine(sender);
                                    sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                                    sender.sendMessage(Component.text(player.getUsername() + " is already shadowbanned!").color(NamedTextColor.RED));
                                    sendLine(sender);
                                }
                            } else {
                                sendLine(sender);
                                sender.sendMessage(Component.text("PistonQueue").color(NamedTextColor.GOLD));
                                sender.sendMessage(Component.text("The player " + args[1] + " was not found!").color(NamedTextColor.GOLD));
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

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();

        if (Config.REGISTER_TAB) {
            final List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                for (String string : commands) {
                    if (string.toLowerCase().startsWith(args[0].toLowerCase()))
                        completions.add(string);
                }

                if (sender.hasPermission(Config.ADMIN_PERMISSION)) {
                    for (String string : adminCommands) {
                        if (string.toLowerCase().startsWith(args[0].toLowerCase()))
                            completions.add(string);
                    }
                }
            } else if (sender.hasPermission(Config.ADMIN_PERMISSION)
                    && args.length == 2
                    && (args[0].equalsIgnoreCase("shadowban") || args[0].equalsIgnoreCase("unshadowban"))) {
                addPlayers(completions, args);
            }

            Collections.sort(completions);

            return completions;
        } else {
            return null;
        }
    }
}
