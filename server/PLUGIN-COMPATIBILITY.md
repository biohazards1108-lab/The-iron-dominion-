# Iron Dominion — Paper 26.3 Plugin Compatibility Register

**Paper server JAR:** [paper-26.3-256.jar](https://fill-data.papermc.io/v1/objects/c86baf83b1fb0eb0d572f39484c242210896c6adf9cc9342d5c16c4de70c9bdc/paper-26.3-256.jar)  
**SHA-256:** `c86baf83b1fb0eb0d572f39484c242210896c6adf9cc9342d5c16c4de70c9bdc`

**Target server:** Paper 26.3 build 256  
**Runtime:** Java 25  
**Hostname:** `the-iron-Dominion.g.akliz.net`  
**Status:** compatibility sources reviewed; no third-party JARs have been downloaded, installed, or runtime-tested in this workspace.

## Install first: cross-play

| Plugin | Source | Version evidence / notes |
|---|---|---|
| Geyser-Spigot | [Official Geyser download](https://geysermc.org/download/?project=geyser) | Official download page dated 20 Nov 2026 lists Java 26.3 and Bedrock 26.40–26.65. Use the Paper/Spigot JAR. |
| Floodgate-Spigot | [Official Floodgate download](https://geysermc.org/download/?project=floodgate) | Required for Bedrock accounts to authenticate without Java accounts. Set Geyser `remote.auth-type: floodgate` and protect `key.pem`. |
| ViaVersion | [Official downloads](https://viaversion.com/downloads.html) · [Hangar project](https://hangar.papermc.io/ViaVersion/ViaVersion) | Optional for Java clients using versions other than the server's 26.3. Geyser's setup guide says ViaVersion is needed if the Java backend is not 26.3; this target is 26.3, so it is not required for Bedrock bridging. |

## Permissions and core administration

| Plugin | Source | Notes |
|---|---|---|
| LuckPerms | [Official site/download](https://luckperms.net/download) · [Installation guide](https://luckperms.net/wiki/Installation) | Recommended for groups and least-privilege permissions. Install only one permissions manager. |

## Custom-content framework and machine addons

These are the versions recorded in the previous Iron Dominion server log and have Paper 26.3 compatibility evidence on their project pages. Install the exact compatible builds as a group only after the cross-play baseline passes.

| Plugin | Source | Paper 26.3 evidence |
|---|---|---|
| Nova | [Hangar](https://hangar.papermc.io/xenondevs/Nova) | `0.25.0-alpha.7` lists 26.3. Framework dependency for the addons below. |
| Machines | [Hangar](https://hangar.papermc.io/xenondevs/Machines) | `0.11.0-alpha.3` lists Paper 26.3 and Nova 0.25.0-alpha.3+. |
| Logistics | [Hangar](https://hangar.papermc.io/xenondevs/Logistics) | `0.9.0-alpha.3` lists Paper 26.3; requires Nova and Simple Upgrades. |
| Simple-Upgrades | [Hangar](https://hangar.papermc.io/xenondevs/Simple-Upgrades) | `1.12.0-alpha.2` lists 26.3. Logistics depends on it. |

These Nova addon builds are alpha releases. Treat them as **in-testing**, not a stable production release. Do not mix addon versions from different Minecraft branches.

## Custom items and RPG systems

| Plugin | Source | Notes |
|---|---|---|
| InfinityAssets | [Hangar](https://hangar.papermc.io/byebye_gg/InfinityAssets) · [Modrinth](https://modrinth.com/plugin/infinityassets) | Project page declares Paper 26.3 support. It creates a resource pack from your own content folders; the plugin ships without Iron Dominion's item content. Keep your content YAML and textures in backups. |
| RPGSkills | [Official project/download](https://chw3021.github.io/rpgskills-site/) · [Installation guide](https://chw3021.github.io/rpgskills-site/installation/) · [Modrinth details](https://modrinth.com/plugin/rpgskills/) | Recommended classes system: 32 classes across 8 archetypes × 4 roles. Use the separate `RPGskills-26.3.jar` build. Its project page lists Paper/Spigot 26.3 and Java 25 for 26.x. Its resource pack is marked Beta and is needed for custom gear/monster visuals. This is a third-party 26.3-compatible build, not a binary rebuilt by Iron Dominion specifically against Paper build 256. |
| AuraSkills | [Official Hangar page](https://hangar.papermc.io/Archy/AuraSkills) | Hangar lists release 2.4.0 for Minecraft 1.20–26.3. Optional. It overlaps with some RPG progression features; start with RPGSkills for the classes work and add AuraSkills only if you deliberately want both systems. |
| ProtocolLib | [Official GitHub releases](https://github.com/dmulloy2/ProtocolLib/releases) | Optional dependency only. Install if a selected plugin explicitly requires it, and choose a release/dev build that explicitly lists 26.3 support. Do not blindly restore an old snapshot. |

## Staged installation order

1. Paper 26.3 build 256 alone.
2. Geyser-Spigot + Floodgate-Spigot.
3. Test Bedrock mobile connection and Survival block breaking/placing.
4. LuckPerms.
5. Nova + Simple-Upgrades + Machines + Logistics (the Nova addon group).
6. InfinityAssets, then test resource-pack merge and client acceptance.
7. RPGSkills.
8. Add optional ViaVersion, AuraSkills, or ProtocolLib only when a clear compatibility need exists.

Restart and test after each stage. If a new stage breaks startup or gameplay, remove only that stage and inspect the first error in the console.

## Explicitly do not install these legacy artifacts

The repository's existing custom projects are not rebuilt for the new server:

- `minecraft-plugin/` — POM targets CraftBukkit 1.6.4-R2.0 and Java 8.
- `npc-plugin/` — POM targets CraftBukkit 1.6.4-R2.0 and Java 6.
- `dominion-tokens/` — POM targets CraftBukkit 1.6.4-R2.0 and Java 8.

They need source migration and a fresh JDK 25 / Paper API 26.3 build 256 compile. No JAR from those projects should be copied to the new server yet.
