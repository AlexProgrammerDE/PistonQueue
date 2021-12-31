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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.pistonmaster.pistonqueue.shared.ComponentWrapper;
import net.pistonmaster.pistonqueue.shared.TextColorWrapper;
import net.pistonmaster.pistonqueue.shared.TextDecorationWrapper;

@Getter
@RequiredArgsConstructor
public final class VelocityComponentWrapperImpl implements ComponentWrapper {
    private final Component mainComponent;

    @Override
    public ComponentWrapper append(String text) {
        return new VelocityComponentWrapperImpl(mainComponent.append(Component.text(text)));
    }

    @Override
    public ComponentWrapper append(ComponentWrapper component) {
        return new VelocityComponentWrapperImpl(mainComponent.append(((VelocityComponentWrapperImpl) component).getMainComponent()));
    }

    @Override
    public ComponentWrapper color(TextColorWrapper color) {
        NamedTextColor namedTextColor = null;

        switch (color) {
            case GOLD:
                namedTextColor = NamedTextColor.GOLD;
                break;
            case RED:
                namedTextColor = NamedTextColor.RED;
                break;
            case DARK_BLUE:
                namedTextColor = NamedTextColor.DARK_BLUE;
                break;
            case GREEN:
                namedTextColor = NamedTextColor.GREEN;
                break;
        }

        return new VelocityComponentWrapperImpl(mainComponent.color(namedTextColor));
    }

    @Override
    public ComponentWrapper decorate(TextDecorationWrapper decoration) {
        TextDecoration textDecoration;

        if (decoration == TextDecorationWrapper.BOLD) {
            textDecoration = TextDecoration.BOLD;
        } else {
            throw new IllegalStateException("Unexpected value: " + decoration);
        }

        return new VelocityComponentWrapperImpl(mainComponent.decorate(textDecoration));
    }
}
