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
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.pistonmaster.pistonqueue.shared.chat.ComponentWrapper;
import net.pistonmaster.pistonqueue.shared.chat.ComponentWrapperFactory;
import net.pistonmaster.pistonqueue.shared.command.MainCommandShared;
import net.pistonmaster.pistonqueue.shared.wrapper.CommandSourceWrapper;
import net.pistonmaster.pistonqueue.velocity.PistonQueueVelocity;

import java.util.List;

@RequiredArgsConstructor
public final class MainCommand implements SimpleCommand, MainCommandShared {
  private final PistonQueueVelocity plugin;

  @Override
  public void execute(Invocation invocation) {
    String[] args = invocation.arguments();
    CommandSource sender = invocation.source();

    onCommand(new CommandSourceWrapper() {
      @Override
      public void sendMessage(ComponentWrapper component) {
        sender.sendMessage(((VelocityComponentWrapperImpl) component).mainComponent());
      }

      @Override
      public boolean hasPermission(String node) {
        return sender.hasPermission(node);
      }
    }, args, plugin);
  }

  @Override
  public List<String> suggest(Invocation invocation) {
    return onTab(invocation.arguments(), invocation.source()::hasPermission, plugin);
  }

  @Override
  public ComponentWrapperFactory component() {
    return text -> new VelocityComponentWrapperImpl(Component.text(text));
  }
}
