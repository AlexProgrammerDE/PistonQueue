package Leees.Bungee.Queue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class ReloadCommand extends Command {

    public ReloadCommand() {
        super("lbq");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1 || args[0].equalsIgnoreCase("help"))  {
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            sender.sendMessage(ChatColor.GOLD + "LeeesBungeeQueue");
            sender.sendMessage(ChatColor.GOLD + "/lbq help");
            sender.sendMessage(ChatColor.GOLD + "/lbq reload");
            sender.sendMessage(ChatColor.GOLD + "/lbq version");
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            return;
        }
        if (args[0].equalsIgnoreCase("version")) {
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            sender.sendMessage(ChatColor.GOLD + "LeeesBungeeQueue");
            sender.sendMessage(ChatColor.GOLD + "Version 3.0.5 by");
            sender.sendMessage(ChatColor.GOLD + "Nate Legault");
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            return;
        }
        if (sender.hasPermission(Lang.ADMINPERMISSION)) {
            if (args[0].equalsIgnoreCase("reload")) {
                QueuePlugin.getInstance().processConfig();
                sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
                sender.sendMessage(ChatColor.GOLD + "LeeesBungeeQueue");
                sender.sendMessage(ChatColor.GREEN + "Config reloaded");
                sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
                return;
            }
            } else {
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            sender.sendMessage(ChatColor.GOLD + "LeeesBungeeQueue");
            sender.sendMessage(ChatColor.RED + "You do not");
            sender.sendMessage(ChatColor.RED + "have permission");
            sender.sendMessage(ChatColor.DARK_BLUE + "----------------");
            }
    }
}