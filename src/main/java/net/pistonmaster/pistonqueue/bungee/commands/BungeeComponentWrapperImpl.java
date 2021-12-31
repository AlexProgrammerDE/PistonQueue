package net.pistonmaster.pistonqueue.bungee.commands;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.pistonmaster.pistonqueue.shared.ComponentWrapper;
import net.pistonmaster.pistonqueue.shared.TextColorWrapper;
import net.pistonmaster.pistonqueue.shared.TextDecorationWrapper;

@Getter
@RequiredArgsConstructor
public class BungeeComponentWrapperImpl implements ComponentWrapper {
    private final ComponentBuilder mainComponent;

    @Override
    public ComponentWrapper append(String text) {
        return new BungeeComponentWrapperImpl(mainComponent.append(text));
    }

    @Override
    public ComponentWrapper append(ComponentWrapper component) {
        return new BungeeComponentWrapperImpl(mainComponent.append(((BungeeComponentWrapperImpl) component).getMainComponent().create()));
    }

    @Override
    public ComponentWrapper color(TextColorWrapper color) {
        ChatColor chatColor = null;
        switch (color) {
            case GOLD:
                chatColor = ChatColor.GOLD;
                break;
            case RED:
                chatColor = ChatColor.RED;
                break;
            case DARK_BLUE:
                chatColor = ChatColor.DARK_BLUE;
                break;
            case GREEN:
                chatColor = ChatColor.GREEN;
                break;
        }

        return new BungeeComponentWrapperImpl(mainComponent.color(chatColor));
    }

    @Override
    public ComponentWrapper decorate(TextDecorationWrapper decoration) {
        if (decoration == TextDecorationWrapper.BOLD) {
            return new BungeeComponentWrapperImpl(mainComponent.bold(true));
        }
        throw new IllegalStateException("Unexpected value: " + decoration);
    }
}
