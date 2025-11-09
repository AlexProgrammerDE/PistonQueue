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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.pistonmaster.pistonqueue.shared.chat.ComponentWrapper;
import net.pistonmaster.pistonqueue.shared.chat.TextColorWrapper;
import net.pistonmaster.pistonqueue.shared.chat.TextDecorationWrapper;

final class BungeeComponentWrapperImpl implements ComponentWrapper {
  private final ComponentBuilder componentBuilder;

  BungeeComponentWrapperImpl(ComponentBuilder componentBuilder) {
    this.componentBuilder = copyBuilder(componentBuilder);
  }

  private BungeeComponentWrapperImpl(ComponentBuilder componentBuilder, boolean trusted) {
    this.componentBuilder = trusted ? componentBuilder : copyBuilder(componentBuilder);
  }

  private static ComponentBuilder copyBuilder(ComponentBuilder source) {
    return new ComponentBuilder(source);
  }

  BaseComponent[] toBaseComponents() {
    return componentBuilder.create();
  }

  @Override
  public ComponentWrapper append(String text) {
    ComponentBuilder newBuilder = copyBuilder(componentBuilder);
    newBuilder.append(text);
    return new BungeeComponentWrapperImpl(newBuilder, true);
  }

  @Override
  public ComponentWrapper append(ComponentWrapper component) {
    ComponentBuilder newBuilder = copyBuilder(componentBuilder);
    BungeeComponentWrapperImpl other = (BungeeComponentWrapperImpl) component;
    newBuilder.append(other.toBaseComponents());
    return new BungeeComponentWrapperImpl(newBuilder, true);
  }

  @Override
  public ComponentWrapper color(TextColorWrapper color) {
    ComponentBuilder newBuilder = copyBuilder(componentBuilder);
    newBuilder.color(switch (color) {
      case GOLD -> ChatColor.GOLD;
      case RED -> ChatColor.RED;
      case DARK_BLUE -> ChatColor.DARK_BLUE;
      case GREEN -> ChatColor.GREEN;
    });
    return new BungeeComponentWrapperImpl(newBuilder, true);
  }

  @Override
  public ComponentWrapper decorate(TextDecorationWrapper decoration) {
    ComponentBuilder newBuilder = copyBuilder(componentBuilder);
    if (decoration == TextDecorationWrapper.BOLD) {
      newBuilder.bold(true);
    }
    return new BungeeComponentWrapperImpl(newBuilder, true);
  }
}
