package net.pistonmaster.pistonqueue.shared.events;

import net.pistonmaster.pistonqueue.shared.PlayerWrapper;

import java.util.Optional;

public interface PQServerConnectedEvent {
    PlayerWrapper getPlayer();

    Optional<String> getPreviousServer();

    String getServer();
}
