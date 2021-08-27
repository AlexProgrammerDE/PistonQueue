package net.pistonmaster.pistonqueue.shared.events;

import net.pistonmaster.pistonqueue.shared.PlayerWrapper;

public interface PQServerPreConnectEvent {
    PlayerWrapper getPlayer();

    String getTarget();

    void setTarget(String server);
}
