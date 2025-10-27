<img align="right" src="https://github.com/AlexProgrammerDE/PistonQueue/blob/main/images/logo.png?raw=true" height="150" width="150">

[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/plugin/pistonqueue)

[![discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-singular_vector.svg)](https://discord.gg/J9bmJNuTJm) [![kofi](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/kofi-singular_vector.svg)](https://ko-fi.com/alexprogrammerde)

# PistonQueue

**⏰️ Powerful queue plugin for anarchy/survival servers.**

## About

PistonQueue is a powerful, but easy to use queue plugin designed for anarchy and survival servers.
It is a recreation of the 2b2t.org queue system design, but adds a lot of features on top.

## Features

* BungeeCord and Velocity support.
* Queue system with reserved slots.
* Shadow-banning players.
* Built-in support for forcing people into the end void.
* Auth server support for cracked (offline mode: false) servers. 
* Joining the auth server first before the queue server.
* Optional multi-endpoint lobby groups with transfer (packet-based external redirect) to load-balance across multiple proxies/nodes.
* Configurable queue position display (chat, action bar, title/subtitle).
* Minimum queue time enforcement to prevent instant joins.
* XP sound notification when approaching front of queue.
* Recovery system to handle connection failures gracefully.

## Setup

Check out the [wiki](https://github.com/AlexProgrammerDE/PistonQueue/wiki) for a tutorial on how to set up PistonQueue.

### Advanced: Lobby groups and packet transfer

If you run multiple Velocity nodes on different public IPs and want basic load balancing and DDoS resilience, you can enable the optional lobby group feature. It lets you define multiple endpoints for a lobby, mixing regular Velocity servers and external nodes, and route players by priority → weight, then break ties by the server with fewer players.

Highlights:
- **Modes**: VELOCITY (connect via proxy server name) and TRANSFER (client is instructed to connect to host:port directly; requires modern clients, e.g. 1.20.5+).
- **Selection**: priority (lower is better), weight (weighted pick within same priority), tie-breaker by least players.
- **Safety**: TRANSFER_MIN_PROTOCOL guard to fall back to proxy connect for older clients.
- **Health checks**: Automatic SLP (Server List Ping) status checks for TRANSFER endpoints.
- **Cooldown**: 10-second transfer cooldown to prevent recovery re-queuing loops.

Configuration is documented inline in `proxy_config.yml` under the section "Advanced: Multi-endpoint lobby groups".

### Queue position display

Players can see their queue position through multiple methods (all configurable):
- **Chat messages**: Traditional text messages with position updates
- **Action bar**: Compact display above the hotbar
- **Title/Subtitle**: Large on-screen display (configurable as title or subtitle)
- **Tab list**: Player list header/footer with position and estimated wait time

Animation timing and update frequency are fully customizable in the config.

### Minimum queue time

Prevent players from instantly joining the main server by enforcing a minimum wait time:
- Configurable minimum seconds before allowing server transfer
- XP sound only plays when queue actually progresses (not during enforced wait)
- Individual entry sound for players joining the queue

### Recovery system

Handles connection failures gracefully:
- Automatic re-queuing if player connection to main server fails
- Position cache to distinguish normal joins from recovery scenarios
- Prevents false "recovery" messages on normal server joins
- Cooldown system to prevent re-queue loops after transfers

## 🌈 Community

Feel free to join our Discord community server:

[![Discord Banner](https://discord.com/api/guilds/739784741124833301/widget.png?style=banner2)](https://discord.gg/J9bmJNuTJm)

This project is in active development, so if you have any feature requests or issues please submit them here on GitHub. PRs are welcome, too. :octocat:
