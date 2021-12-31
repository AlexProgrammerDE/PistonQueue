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
