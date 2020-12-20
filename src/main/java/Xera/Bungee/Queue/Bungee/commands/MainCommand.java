package Xera.Bungee.Queue.Bungee.commands;

import Xera.Bungee.Queue.Bungee.utils.Config;
import Xera.Bungee.Queue.Bungee.QueueAPI;
import Xera.Bungee.Queue.Bungee.XeraBungeeQueue;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MainCommand extends Command implements TabExecutor {
    private final XeraBungeeQueue plugin;
    private static final String[] COMMANDS = { "help", "version", "stats", "reload" };

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
                        if (args.length > 1 && plugin.getProxy().getPlayer(args[1]) != null) {
                            if (args.length > 2) {

                            } else {
                                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                                sender.sendMessage(new ComponentBuilder("You forgot the time!").color(ChatColor.GOLD).create());
                                sender.sendMessage(new ComponentBuilder("/xbq shadowban player hours [minutes]").color(ChatColor.GOLD).create());
                                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                            }
                        } else {
                            sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                            sender.sendMessage(new ComponentBuilder("The player " + args[1] + " was not found!").color(ChatColor.GOLD).create());
                            sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
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
        }

        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (Config.REGISTERTAB) {
            final List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                for (String string : COMMANDS)
                    if (string.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(string);
            } else if (sender.hasPermission(Config.SHADOWBANPERMISSION) &&
                    args.length == 2 &&
                    args[0].equalsIgnoreCase("shadowban")) {
                for (Player player : Bukkit.getOnlinePlayers())
                    if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) completions.add(player.getName());
            }

            Collections.sort(completions);

            return completions;
        } else {
            return null;
        }
    }
}