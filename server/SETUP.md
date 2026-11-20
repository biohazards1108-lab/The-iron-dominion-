# Iron Dominion Server Setup — Akliz Rebuild

**Target:** Paper 26.3 build 256, Java 25, Akliz hostname `the-iron-Dominion.g.akliz.net`.

The owner has reprovisioned the server. This guide is for the fresh installation; do not restore old server binaries or legacy plugin JARs blindly.

## 1. Confirm the Akliz allocations first

The hostname alone does not reveal the connection ports. In the Akliz panel, record both:

- **Java/Paper port** — the port assigned to the Paper server.
- **Bedrock/Geyser port** — a separate allocation for Geyser.

Do not reuse the retired hostname or the old port `15300`. Do not assume the default Geyser port `19132` is the externally assigned Akliz port.

For current Geyser NetherNet transport, the Geyser port must be reachable over both TCP and UDP. Confirm Akliz exposes both protocols for the assigned Bedrock port.

## 2. Install the server software

1. Set the Akliz Java runtime to **Java 25**.
2. Select **Paper 26.3 build 256** in Akliz if the panel offers that exact build.
3. If Akliz does not offer it, download the official Paper server JAR from [PaperMC Downloads](https://papermc.io/downloads/paper) and select Minecraft `26.3` / build `256`. Upload it and set the panel's startup JAR to the uploaded filename.
4. Start the server once with no plugins. Let it generate its files, review the console, then stop it cleanly.
5. Accept the Minecraft EULA only if you agree to it.
6. Keep Akliz's assigned Java port and startup arguments intact. Do not overwrite the provider's `server.properties` with a generic template.

Paper 26.1+ requires Java 25. Paper's developer documentation identifies build-specific API dependencies as `26.3.build.256-stable` when compiling a plugin for this exact server build.

## 3. Install cross-play before other plugins

Download the Paper/Spigot variants only:

- [Geyser-Spigot](https://geysermc.org/download/?project=geyser)
- [Floodgate-Spigot](https://geysermc.org/download/?project=floodgate)

Upload both JARs to `plugins/` and fully restart. Do not use Fabric, NeoForge, Velocity, or Standalone variants on this Paper server.

After first startup, open `plugins/Geyser-Spigot/config.yml`:

- Set `bedrock.port` to the **Bedrock/Geyser port assigned by Akliz**, not the Paper Java port.
- Keep the generated defaults for the other Geyser fields unless the official setup guide requires a change.
- Set `remote.auth-type: floodgate` in the generated `remote` section.

Restart the server. Keep Floodgate's generated `key.pem` private. Never upload it to GitHub, share it in Discord, or send it in a support ticket.

## 4. Confirm the basic server before adding content

Test with the minimum plugin set:

1. Paper only: console starts and stops cleanly.
2. Paper + Geyser + Floodgate: console starts without errors.
3. Bedrock mobile player joins through the assigned Geyser port.
4. Test Survival block breaking, block placement, chat, inventory, death/respawn, and resource-pack prompt.
5. Confirm Java players can join on Java 26.3 if a Java tester is available.

Do not add the content plugins until this baseline works.

## 5. Add the remaining plugins in stages

See `PLUGIN-COMPATIBILITY.md` for the reviewed list and exact order. Add one plugin or one tightly coupled addon group at a time, restart, and test. Do not upload every plugin at once.

## 6. Initial safety settings

- Keep the allowlist enabled while testing with the five admins.
- Keep `online-mode=true` for Java authentication; Floodgate handles Bedrock authentication through Geyser.
- Set Survival and Normal difficulty intentionally in Akliz/server settings.
- Keep spawn protection disabled only during controlled tests; configure a deliberate spawn protection policy before opening the server.
- Enable Akliz backups before installing content packs or generating important builds.
- Never expose RCON or provider credentials publicly.

## 7. Custom Iron Dominion JARs — blocked pending migration

The repository currently contains legacy plugin source projects under `minecraft-plugin/`, `npc-plugin/`, and `dominion-tokens/`. Their POM files target CraftBukkit 1.6.4 and Java 6/8. They are **not** Paper 26.3 plugins and must not be installed.

A new custom-plugin build must use JDK 25, Java release 25, and Paper API `io.papermc.paper:paper-api:26.3.build.256-stable`. The source must also be migrated from removed/changed legacy APIs, then compiled, inspected, and tested on the fresh server. No legacy JAR is being represented as a compatible build.

## Release checklist

- [ ] Akliz panel confirms Java 25.
- [ ] Akliz panel confirms Paper 26.3 build 256.
- [ ] Java/Paper port recorded.
- [ ] Separate Geyser port recorded and exposed on TCP + UDP.
- [ ] Paper-only startup verified.
- [ ] Geyser + Floodgate startup verified.
- [ ] Bedrock mobile joins successfully.
- [ ] Bedrock Survival breaking and placing work.
- [ ] Java client connection tested if available.
- [ ] Resource-pack prompt and custom item rendering tested.
- [ ] Backup and restore procedure tested.
- [ ] Website ports updated only after the above checks.
