package net.pistonmaster.pistonqueue.shared.loadbalance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LobbyGroupConfig {
  private String name;
  private List<EndpointConfig> endpoints = new ArrayList<>();
  private SelectionOptions selection = new SelectionOptions();
}
