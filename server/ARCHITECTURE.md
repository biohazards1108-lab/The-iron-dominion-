# Bedrock Dominion — Server Architecture Plan

**Status: fresh Akliz-hosted Paper server provisioned by owner; rebuild target is Paper 26.3 build 256 on Java 25. New hostname: `the-iron-Dominion.g.akliz.net`. Assigned Java and Geyser ports and successful client tests are pending.**

This file tracks the current Java Paper backend with Geyser/Floodgate cross-play. Old Bukkit/Cauldron/Tekkit plugins in this repository are not automatically compatible with Paper 26.3 and must not be installed until migrated and tested.

## 1. Current server model

There are two different goals that should not be confused:

| Approach | What it means | Main trade-off |
| --- | --- | --- |
| **Bedrock-native server** | Run official Bedrock Dedicated Server software. | Not the current rebuild target; Java Paper plugins do not run on this server type. |
| **Cross-play Java server** | Run Paper 26.3 and use Geyser-Spigot plus Floodgate-Spigot. | Current target. Java players connect directly to Paper; Bedrock players connect through Geyser. Both paths require testing. |
| **Separate edition servers** | Run distinct Java and Bedrock servers and connect them at the community level. | More hosting, administration, backups, and potentially separate gameplay/economy systems. Shared identity or progress is not automatic. |

### Provisional recommendation

The current rebuild target is an Akliz-hosted Paper 26.3 build 256 server running on Java 25. Owner-supplied hostname: `the-iron-Dominion.g.akliz.net`. Geyser-Spigot and Floodgate-Spigot are the intended Bedrock bridge. The Java port and Geyser Bedrock port are not yet supplied; obtain both from the Akliz panel. Do not reuse the old hostname or old port 15300. Mark the server **setup in progress** until the panel ports, plugin startup, and Java/Bedrock client connections are tested.

Do not promise a single shared world, inventory, economy, or player identity across editions unless that behavior is implemented and verified.

## 2. Decisions required before implementation

- [x] Primary server model: Paper 26.3 Java backend with Geyser-Spigot and Floodgate-Spigot for Bedrock cross-play.
- [ ] Hosting provider and server operating system.
- [x] Target Minecraft version: Java 26.3 / Paper build 256; Java runtime 25. Confirm Bedrock client compatibility against current Geyser support.
- [ ] Expected player capacity and performance budget.
- [ ] Confirm Akliz Java port and separate Geyser Bedrock port for `the-iron-Dominion.g.akliz.net`.
- [ ] World type, difficulty, PvP policy, grief protection, and reset policy.
- [ ] Backup schedule, retention, and restore test.
- [ ] Admin/staff permissions, moderation process, and player reporting path.
- [ ] Whether any economy, ranks, voting rewards, or store will exist at launch.
- [ ] Required behavior/resource packs and the process for testing client compatibility.

## 3. Bedrock content plan

For the current Paper setup, design content as Paper plugins, Nova addons, or InfinityAssets content packs. Bedrock clients receive the translated experience through Geyser; every custom item, GUI, resource pack, and interaction must be tested on both Java and Bedrock clients. Do not assume legacy Bukkit/Cauldron 1.6.4 JARs are compatible.

For every custom feature, keep a small release record:

1. Feature name and player-facing purpose.
2. Required server software and Minecraft version.
3. Whether it needs a behavior pack, resource pack, server extension, or external service.
4. Client-side requirements and compatibility test results.
5. Permissions, abuse cases, and rollback method.
6. Status: **planned**, **in testing**, or **released**.

Do not publish downloads or call a feature live until the matching build has been tested on the actual target server and client versions.

## 4. Secure baseline before opening the server

- Keep server administration restricted to trusted operators and least-privilege accounts.
- Enable an allowlist/whitelist during private testing; decide the public-access policy before launch.
- Never commit world data, private keys, RCON credentials, API tokens, or provider credentials.
- Do not expose RCON or other administrative interfaces to the public internet.
- Keep the server software and add-ons updated; review every third-party download before installation.
- Back up world data and configuration before updates; periodically test restoration.
- Log administrative actions and investigate suspicious activity without publishing private player data.
- Test permissions, grief protections, crash recovery, restart behavior, and backups before inviting players.
- Keep the store, payment fulfillment, voting rewards, and token economy disabled until a secure, supported implementation exists.

## 5. Website and server integration

The public website is static. It must not be treated as a trusted backend, account system, payment processor, or live server-status source.

Only connect live status after the chosen server can provide verified status data. Only enable economy, ranks, voting, or purchases after server-side authentication, durable transaction records, duplicate-event protection, and a tested delivery process exist.

Never place private server credentials in browser JavaScript. Never use a player-entered username as proof of account ownership.

## 6. Release checklist

- [ ] Server model and supported versions confirmed.
- [ ] Private test server starts, stops, and restarts cleanly.
- [ ] Connection tested from each supported client edition and platform.
- [ ] Add-ons/features tested with a clean client and documented.
- [ ] Allowlist, operator permissions, moderation, and backup restoration tested.
- [ ] Performance tested with a representative number of players.
- [ ] Public address and port verified from an external network.
- [ ] Website connection instructions match the real server configuration.
- [ ] Live status reports verified data, not placeholder values.
- [ ] Store/economy/voting remain disabled unless separately approved and tested.
- [ ] Launch announcement published only after all required checks pass.
