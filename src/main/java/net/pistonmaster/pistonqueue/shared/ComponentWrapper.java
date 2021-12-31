package net.pistonmaster.pistonqueue.shared;

public interface ComponentWrapper {
    ComponentWrapper append(String text);

    ComponentWrapper append(ComponentWrapper component);

    ComponentWrapper color(TextColorWrapper color);

    ComponentWrapper decorate(TextDecorationWrapper decoration);
}
