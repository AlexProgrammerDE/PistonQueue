package net.pistonmaster.pistonqueue.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface MainCommandShared {
     String[] commands = {"help", "version", "stats"};
     String[] adminCommands = {"slotstats", "reload", "shadowban", "unshadowban"};

     default List<String> onTab(String[] args, PermissibleWrapper wrapper, PistonQueueProxy plugin) {
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
             return null;
         }
     }

    default void addPlayers(List<String> completions, String[] args, PistonQueueProxy proxy) {
        for (PlayerWrapper player : proxy.getPlayers()) {
            if (player.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                completions.add(player.getName());
        }
    }
}
