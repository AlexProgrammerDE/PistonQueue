package net.pistonmaster.pistonqueue.shared.loadbalance;

public enum PlayerCountSource {
  AUTO,            // VELOCITY: proxy player list; TRANSFER: status ping (fallback to recent transfers)
  STATUS_PING,     // Always try status ping
  RECENT_TRANSFER  // Use recent transfer counts only
}
