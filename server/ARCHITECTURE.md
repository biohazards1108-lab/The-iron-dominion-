# Bedrock Dominion — Server Architecture Plan

**Status: planning only. No production server is configured or launched by this document.**

This file keeps the server plan honest while the project moves from the previous Java/Bukkit-era setup to Bedrock Dominion. The old plugins and Node backend in this repository are not Bedrock-compatible by default and must not be installed into a new server.

## 1. Decide what “Bedrock server” means

There are two different goals that should not be confused:

| Approach | What it means | Main trade-off |
| --- | --- | --- |
| **Bedrock-native server** | Run official Bedrock Dedicated Server software. | Bedrock clients are the native target. Java client access is not assumed and must not be promised without a tested, supported bridge. |
| **Cross-play Java server** | Run a compatible Java server and use a bridge such as Geyser, with Floodgate if the chosen setup requires it. | Bedrock clients can connect to a Java backend, but this is not the same as running a native Bedrock Dedicated Server. Plugin and gameplay compatibility must be tested. |
| **Separate edition servers** | Run distinct Java and Bedrock servers and connect them at the community level. | More hosting, administration, backups, and potentially separate gameplay/economy systems. Shared identity or progress is not automatic. |

### Provisional recommendation

Treat Bedrock-native gameplay as the primary requirement until the owner explicitly chooses otherwise. Keep Java cross-play marked **planned / unconfirmed** on the website. Before writing server-specific plugins or publishing connection details, select the server software and test the intended client editions against it.

Do not promise a single shared world, inventory, economy, or player identity across editions unless that behavior is implemented and verified.

## 2. Decisions required before implementation

- [ ] Primary server model: Bedrock-native, Java backend with Bedrock bridge, or separate servers.
- [ ] Hosting provider and server operating system.
- [ ] Target Minecraft version(s), chosen only after checking current official releases and compatibility.
- [ ] Expected player capacity and performance budget.
- [ ] Public connection address, port, and DNS plan.
- [ ] World type, difficulty, PvP policy, grief protection, and reset policy.
- [ ] Backup schedule, retention, and restore test.
- [ ] Admin/staff permissions, moderation process, and player reporting path.
- [ ] Whether any economy, ranks, voting rewards, or store will exist at launch.
- [ ] Required behavior/resource packs and the process for testing client compatibility.

## 3. Bedrock content plan

For a Bedrock-native setup, design content around the actual capabilities of the selected Bedrock server and its supported add-on/behavior-pack/resource-pack systems. Do not assume Java Bukkit, Spigot, Paper, Cauldron, Forge, or Fabric plugins can run on Bedrock.

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
