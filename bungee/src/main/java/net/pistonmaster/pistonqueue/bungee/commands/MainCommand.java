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
package net.pistonmaster.pistonqueue.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.pistonmaster.pistonqueue.bungee.PistonQueueBungee;
import net.pistonmaster.pistonqueue.shared.CommandSourceWrapper;
import net.pistonmaster.pistonqueue.shared.ComponentWrapper;
import net.pistonmaster.pistonqueue.shared.ComponentWrapperFactory;
import net.pistonmaster.pistonqueue.shared.MainCommandShared;

public final class MainCommand extends Command implements TabExecutor, MainCommandShared {
    private final PistonQueueBungee plugin;

    public MainCommand(PistonQueueBungee plugin) {
        super("pistonqueue", null, "pq");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        onCommand(new CommandSourceWrapper() {
            @Override
            public void sendMessage(ComponentWrapper component) {
                sender.sendMessage(((BungeeComponentWrapperImpl) component).mainComponent().create());
            }

            @Override
            public boolean hasPermission(String node) {
                return sender.hasPermission(node);
            }
        }, args, plugin);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return onTab(args, sender::hasPermission, plugin);
    }

    @Override
    public ComponentWrapperFactory component() {
        return text -> new BungeeComponentWrapperImpl(new ComponentBuilder(text));
    }
}
