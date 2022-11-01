package net.pistonmaster.pistonqueue.shared.events;

public interface PQPreLoginEvent {
    boolean isCancelled();

    void setCancelled(String reason);

    String getUsername();
}
