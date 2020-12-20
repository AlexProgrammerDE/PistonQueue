package ca.xera.bungee.queue.bungee.commands;

import ca.xera.bungee.queue.bungee.utils.Config;
import ca.xera.bungee.queue.bungee.QueueAPI;
import ca.xera.bungee.queue.bungee.XeraBungeeQueue;
import ca.xera.bungee.queue.bungee.utils.StorageTool;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;

public final class MainCommand extends Command implements TabExecutor {
    private final XeraBungeeQueue plugin;
    private static final String[] commands = { "help", "version", "stats" };
    private static final String[] adminCommands = { "reload", "shadowban", "unshadowban" };

    public MainCommand(XeraBungeeQueue plugin) {
        super("xbq");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            help(sender);
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "help":
                    help(sender);
                    break;
                case "version":
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder("Version " + plugin.getDescription().getVersion() + " by").color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder(plugin.getDescription().getAuthor()).color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    break;
                case "stats":
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    sender.sendMessage(new ComponentBuilder("Queue stats").color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder("Regular: ").color(ChatColor.GOLD).append(String.valueOf(QueueAPI.getRegularSize())).color(ChatColor.GOLD).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("Priority: ").color(ChatColor.GOLD).append(String.valueOf(QueueAPI.getPrioritySize())).color(ChatColor.GOLD).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("Veteran: ").color(ChatColor.GOLD).append(String.valueOf(QueueAPI.getVeteranSize())).color(ChatColor.GOLD).bold(true).create());
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    break;
                case "reload":
                    if (sender.hasPermission(Config.ADMINPERMISSION)) {
                        plugin.processConfig();

                        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                        sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                        sender.sendMessage(new ComponentBuilder("Config reloaded").color(ChatColor.GREEN).create());
                        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    } else {
                        noPermission(sender);
                    }
                    break;
                case "shadowban":
                    if (sender.hasPermission(Config.ADMINPERMISSION)) {
                        if (args.length > 1) {
                            if (plugin.getProxy().getPlayer(args[1]) != null) {
                                ProxiedPlayer player = plugin.getProxy().getPlayer(args[1]);

                                if (args.length > 2) {
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTime(new Date());

                                    if (args[2].toLowerCase().endsWith("d")) {
                                        int d = Integer.parseInt(args[2].toLowerCase().replaceAll("d", ""));

                                        calendar.add(Calendar.DAY_OF_WEEK, d);
                                    } else if (args[2].toLowerCase().endsWith("h")) {
                                        int h = Integer.parseInt(args[2].toLowerCase().replaceAll("h", ""));

                                        calendar.add(Calendar.HOUR_OF_DAY, h);
                                    } else if (args[2].toLowerCase().endsWith("m")) {
                                        int m = Integer.parseInt(args[2].toLowerCase().replaceAll("m", ""));

                                        calendar.add(Calendar.MINUTE, m);
                                    } else {
                                        sendBanHelp(sender);
                                        break;
                                    }

                                    if (StorageTool.shadowBanPlayer(player, calendar.getTime())) {
                                        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                        sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                                        sender.sendMessage(new ComponentBuilder("Successfully shadowbanned " + player.getName() + "!").color(ChatColor.GREEN).create());
                                        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                    } else {
                                        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                        sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                                        sender.sendMessage(new ComponentBuilder(player.getName() + " is already shadow banned!").color(ChatColor.RED).create());
                                        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                    }
                                } else {
                                    sendBanHelp(sender);
                                }
                            } else {
                                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                                sender.sendMessage(new ComponentBuilder("The player " + args[1] + " was not found!").color(ChatColor.GOLD).create());
                                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                            }
                        } else {
                            sendBanHelp(sender);
                        }
                    } else {
                        noPermission(sender);
                    }
                    break;
                case "unshadowban":
                    if (sender.hasPermission(Config.ADMINPERMISSION)) {
                        if (args.length > 1) {
                            if (plugin.getProxy().getPlayer(args[1]) != null) {
                                ProxiedPlayer player = plugin.getProxy().getPlayer(args[1]);

                                if (StorageTool.unShadowBanPlayer(player)) {
                                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                    sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                                    sender.sendMessage(new ComponentBuilder("Successfully unshadowbanned " + player.getName() + "!").color(ChatColor.GREEN).create());
                                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                } else {
                                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                    sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                                    sender.sendMessage(new ComponentBuilder(player.getName() + " is already shadow banned!").color(ChatColor.RED).create());
                                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                }
                            } else {
                                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                                sender.sendMessage(new ComponentBuilder("The player " + args[1] + " was not found!").color(ChatColor.GOLD).create());
                                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                            }
                        } else {
                            sendUnBanHelp(sender);
                        }
                    } else {
                        noPermission(sender);
                    }
                    break;
            }
        }
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
        sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("You do not").color(ChatColor.RED).create());
        sender.sendMessage(new ComponentBuilder("have permission").color(ChatColor.RED).create());
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
    }

    private void help(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
        sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq help").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq version").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq stats").color(ChatColor.GOLD).create());

        if (sender.hasPermission(Config.ADMINPERMISSION)) {
            sender.sendMessage(new ComponentBuilder("/xbq reload").color(ChatColor.GOLD).create());
            sender.sendMessage(new ComponentBuilder("/xbq shadowban").color(ChatColor.GOLD).create());
            sender.sendMessage(new ComponentBuilder("/xbq unshadowban").color(ChatColor.GOLD).create());
        }

        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
    }

    private void sendBanHelp(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
        sender.sendMessage(new ComponentBuilder("/xbq shadowban player <d|h|m>").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("Example:").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq shadowban Pistonmaster 2d").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
    }

    private void sendUnBanHelp(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
        sender.sendMessage(new ComponentBuilder("/xbq unshadowban player").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("Example:").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq unshadowban Pistonmaster").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (Config.REGISTERTAB) {
            final List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                for (String string : commands) {
                    if (string.toLowerCase().startsWith(args[0].toLowerCase()))
                        completions.add(string);
                }

                if (sender.hasPermission(Config.ADMINPERMISSION)) {
                    for (String string : adminCommands) {
                        if (string.toLowerCase().startsWith(args[0].toLowerCase()))
                            completions.add(string);
                    }
                }
            } else if (sender.hasPermission(Config.ADMINPERMISSION) &&
                    args.length == 2 &&
                    args[0].equalsIgnoreCase("shadowban")) {
                for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                        completions.add(player.getName());
                }

            }

            Collections.sort(completions);

            return completions;
        } else {
            return null;
        }
    }
}