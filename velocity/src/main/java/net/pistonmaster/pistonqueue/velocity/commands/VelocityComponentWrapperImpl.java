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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.pistonmaster.pistonqueue.shared.chat.ComponentWrapper;
import net.pistonmaster.pistonqueue.shared.chat.TextColorWrapper;
import net.pistonmaster.pistonqueue.shared.chat.TextDecorationWrapper;

public record VelocityComponentWrapperImpl(Component mainComponent) implements ComponentWrapper {
  @Override
  public ComponentWrapper append(String text) {
    return new VelocityComponentWrapperImpl(mainComponent.append(Component.text(text)));
  }

  @Override
  public ComponentWrapper append(ComponentWrapper component) {
    return new VelocityComponentWrapperImpl(mainComponent.append(((VelocityComponentWrapperImpl) component).mainComponent()));
  }

  @Override
  public ComponentWrapper color(TextColorWrapper color) {
    return new VelocityComponentWrapperImpl(mainComponent.color(switch (color) {
      case GOLD -> NamedTextColor.GOLD;
      case RED -> NamedTextColor.RED;
      case DARK_BLUE -> NamedTextColor.DARK_BLUE;
      case GREEN -> NamedTextColor.GREEN;
    }));
  }

  @Override
  public ComponentWrapper decorate(TextDecorationWrapper decoration) {
    return new VelocityComponentWrapperImpl(mainComponent.decorate(switch (decoration) {
      case BOLD -> TextDecoration.BOLD;
    }));
  }
}
