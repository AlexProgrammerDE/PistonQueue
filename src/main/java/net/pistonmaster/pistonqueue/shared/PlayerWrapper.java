package net.pistonmaster.pistonqueue.shared;

import java.util.Optional;
import java.util.UUID;

public interface PlayerWrapper {
    boolean hasPermission(String node);
    void connect(String server);
    Optional<String> getCurrentServer();
    void sendMessage(String message);
    void sendActionBar(String message);
    UUID getUniqueId();
}
