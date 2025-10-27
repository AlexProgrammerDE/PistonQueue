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

## Setup

Check out the [wiki](https://github.com/AlexProgrammerDE/PistonQueue/wiki) for a tutorial on how to set up PistonQueue.

### Advanced: Lobby groups and packet transfer

If you run multiple Velocity nodes on different public IPs and want basic load balancing and DDoS resilience, you can enable the optional lobby group feature. It lets you define multiple endpoints for a lobby, mixing regular Velocity servers and external nodes, and route players by priority → weight, then break ties by the server with fewer players.

Highlights:
- Modes: VELOCITY (connect via proxy server name) and TRANSFER (client is instructed to connect to host:port directly; requires modern clients, e.g. 1.20.5+).
- Selection: priority (lower is better), weight (weighted pick within same priority), tie-breaker by least players.
- Safety: TRANSFER_MIN_PROTOCOL guard to fall back to proxy connect for older clients.

Configuration is documented inline in `proxy_config.yml` under the section "Advanced: Multi-endpoint lobby groups".

## 🌈 Community

Feel free to join our Discord community server:

[![Discord Banner](https://discord.com/api/guilds/739784741124833301/widget.png?style=banner2)](https://discord.gg/J9bmJNuTJm)

This project is in active development, so if you have any feature requests or issues please submit them here on GitHub. PRs are welcome, too. :octocat:
