package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainCommand extends Command implements TabExecutor {
    XeraBungeeQueue plugin;
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
            if (args[0].equalsIgnoreCase("help"))  {
                help(sender);
            }

            if (args[0].equalsIgnoreCase("version")) {
                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder("Version " + plugin.getDescription().getVersion() + " by").color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder(plugin.getDescription().getAuthor()).color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
            }

            if (args[0].equalsIgnoreCase("stats")) {
                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                sender.sendMessage(new ComponentBuilder("Queue stats").color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder("Regular: ").color(ChatColor.GOLD).append(String.valueOf(API.getRegularSize())).color(ChatColor.GOLD).bold(true).create());
                sender.sendMessage(new ComponentBuilder("Priority: ").color(ChatColor.GOLD).append(String.valueOf(API.getPrioritySize())).color(ChatColor.GOLD).bold(true).create());
                sender.sendMessage(new ComponentBuilder("Veteran: ").color(ChatColor.GOLD).append(String.valueOf(API.getVeteranSize())).color(ChatColor.GOLD).bold(true).create());
                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission(Config.ADMINPERMISSION)) {
                    plugin.processConfig();

                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder("Config reloaded").color(ChatColor.GREEN).create());
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                } else {
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder("You do not").color(ChatColor.RED).create());
                    sender.sendMessage(new ComponentBuilder("have permission").color(ChatColor.RED).create());
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                }
            }
        }
    }

    void help(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
        sender.sendMessage(new ComponentBuilder("XeraBungeeQueue").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq help").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq reload").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq version").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/xbq stats").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (Config.REGISTERTAB) {
            final List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                for (String string : COMMANDS)
                    if (string.toLowerCase().startsWith(args[0].toLowerCase())) completions.add(string);
            }

            Collections.sort(completions);

            return completions;
        } else {
            return null;
        }
    }
}