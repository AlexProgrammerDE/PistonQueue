package net.pistonmaster.pistonqueue.shared;

public interface CommandSourceWrapper extends PermissibleWrapper {
    void sendMessage(ComponentWrapper component);
}
