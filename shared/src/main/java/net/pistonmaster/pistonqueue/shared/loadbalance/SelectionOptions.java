package net.pistonmaster.pistonqueue.shared.loadbalance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SelectionOptions {
  private TieBreaker tieBreaker = TieBreaker.LEAST_PLAYERS;
  private PlayerCountSource playerCountSource = PlayerCountSource.AUTO;
  private TransportOverride transportOverride = TransportOverride.PER_ENDPOINT;
  private TransferFallback fallbackOnTransferFail = TransferFallback.PROXY_CONNECT;
  private int pingTimeoutMs = 750;
  private int cacheTtlMs = 2000;
}
