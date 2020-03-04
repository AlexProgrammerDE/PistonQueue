# LeeesBungeeQueue
LeeesBungeeQueue is a 2b2t like queue plugin for Bungeecord to limit the amount of players on the main server because of lag or because its too many and people who try to join will wait in a separate queue server in a line to join the main one the queue should be a server where they are stuck in one place so that no one can move and load chunks so that the queue isnt laggy at all



config should look like this for bungeecord if using my premade queue make ur main servers port 8303

connection_throttle_limit: -1
online_mode: true
log_commands: false
network_compression_threshold: 512
listeners:
- query_port: 25567
  motd: '&6Proxy server'
  tab_list: GLOBAL_PING
  query_enabled: true
  proxy_protocol: false
  forced_hosts:
    old.6b6t.org: 6b6t
  ping_passthrough: false
  priorities:
  - main
  bind_local_address: true
  host: 0.0.0.0:25565
  max_players: 9000
  tab_size: 300
  force_default_server: true
connection_throttle: -1
groups:
log_pings: false
ip_forward: true
prevent_proxy_connections: false
forge_support: true
stats: 6fcdf850-bb47-47dc-8b1d-4427b483c35c
disabled_commands:
- disabledcommandhere
timeout: 30000
permissions:
  default: null
  admin:
  - bungeecord.command.server
  - bungeecord.command.alert
  - bungeecord.command.end
  - bungeecord.command.ip
  - bungeecord.command.reload
servers:
  6b6t:
    motd: '&b6b&36t &66Main Proxy Default Motd Cunts'
    address: 127.0.0.1:8303
    restricted: true
  queue:
    motd: '&b6b&36t &66Queue Proxy Default Motd Cunts'
    address: 127.0.0.1:8283
    restricted: false
player_limit: 9000
