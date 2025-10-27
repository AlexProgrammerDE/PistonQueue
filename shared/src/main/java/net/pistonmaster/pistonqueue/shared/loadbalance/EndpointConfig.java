package net.pistonmaster.pistonqueue.shared.loadbalance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndpointConfig {
  private String name;
  private EndpointMode mode = EndpointMode.VELOCITY;
  private int priority = 1;
  private int weight = 1;

  // VELOCITY mode
  private String velocityServer;

  // TRANSFER mode
  private String host;
  private int port;
}
