package net.pistonmaster.pistonqueue.shared.loadbalance;

public enum TransferFallback {
  PROXY_CONNECT, // Fallback to proxy connect when transfer not possible
  ABORT          // Abort and let caller decide (e.g., requeue)
}
