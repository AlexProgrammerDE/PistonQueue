package Xera.Bungee.Queue.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class ReloadCommand extends Command {
    XeraBungeeQueue plugin;

    public ReloadCommand(XeraBungeeQueue plugin) {
        super("lbq");
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
                sender.sendMessage(new ComponentBuilder("XeraBungeeQueueBukkit").color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder("Version " + plugin.getDescription().getVersion() + " by").color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder(plugin.getDescription().getAuthor()).color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
            }

            if (args[0].equalsIgnoreCase("stats")) {
                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                sender.sendMessage(new ComponentBuilder("Queue stats").color(ChatColor.GOLD).create());
                sender.sendMessage(new ComponentBuilder("Priority: ").color(ChatColor.GOLD).append(String.valueOf(XeraBungeeQueue.priorityqueue.size())).color(ChatColor.GOLD).bold(true).create());
                sender.sendMessage(new ComponentBuilder("Regular: ").color(ChatColor.GOLD).append(String.valueOf(XeraBungeeQueue.regularqueue.size())).color(ChatColor.GOLD).bold(true).create());
                sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission(Config.ADMINPERMISSION)) {
                    plugin.processConfig();

                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    sender.sendMessage(new ComponentBuilder("XeraBungeeQueueBukkit").color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder("Config reloaded").color(ChatColor.GREEN).create());
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                } else {
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                    sender.sendMessage(new ComponentBuilder("XeraBungeeQueueBukkit").color(ChatColor.GOLD).create());
                    sender.sendMessage(new ComponentBuilder("You do not").color(ChatColor.RED).create());
                    sender.sendMessage(new ComponentBuilder("have permission").color(ChatColor.RED).create());
                    sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
                }
            }
        }
    }

    void help(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
        sender.sendMessage(new ComponentBuilder("XeraBungeeQueueBukkit").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/lbq help").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/lbq reload").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/lbq version").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("/lbq stats").color(ChatColor.GOLD).create());
        sender.sendMessage(new ComponentBuilder("----------------").color(ChatColor.DARK_BLUE).create());
    }
}