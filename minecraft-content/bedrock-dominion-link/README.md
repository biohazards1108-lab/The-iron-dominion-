# Bedrock Dominion Website Link behavior pack

This behavior pack links a signed-in Iron Dominion website account to a player who actually joins the Bedrock Dedicated Server and submits that account's one-time code. Microsoft website OAuth alone is not treated as proof of Minecraft ownership.

## Important compatibility warning

This pack uses the pre-release `@minecraft/server-net` API documented for Bedrock Dedicated Server 1.26.60. It must be tested on a backup world/server before production. It is not a Java/Bukkit plugin and must not be placed in a Java server's plugins folder. If Akliz's selected server build or world settings do not permit the required script modules, this exact integration cannot run until those server settings are enabled or the hosting plan supports them.

## Server configuration

1. Back up the world before enabling any behavior pack or script API.
2. Install/enable this behavior pack on the Bedrock world using the Akliz-supported world/behavior-pack workflow.
3. Follow Microsoft's current Bedrock Dedicated Server scripting guide to allow the script module and `@minecraft/server-net` in the pack-specific `permissions.json`. Use `config/permissions.json.example` as a reference; do not overwrite other existing permissions blindly.
4. Configure the server variable `dominionLinkEndpoint` to `https://dfdzjfsydaoovkxhilov.supabase.co/functions/v1/minecraft-link`.
5. Add the generated `dominionServerToken` value to the server's protected `secrets.json`. Do not commit the real value or share it with players.
6. Configure the same random token as the Supabase Edge Function secret `DOMINION_SERVER_TOKEN`. It must be at least 32 random characters. Never put this token in website JavaScript or a public resource pack.
7. Deploy the Edge Function and apply the new SQL migration before testing.
8. Restart the server and test with a non-admin account and a fresh website test account.

## Player workflow

1. Sign in at https://theirondominion.com and open the player dashboard.
2. Select **Link Minecraft** and generate a code. It expires after 10 minutes and can only be used once.
3. Join Bedrock Dominion with the account you want to link.
4. Type `!link YOURCODE` in in-game chat.
5. Return to the dashboard and refresh link status.

Only the server script can claim a code, and the server token is kept in the server's protected secret store. The code itself is never stored in plaintext in the database. A successful link writes a verified Bedrock identity row; the player must still be in control of both the website session and the in-game account at the time of linking.

## Rollback

Disable the behavior pack in the world settings and restart. Do not delete player data or the world. Revoke/rotate the server token if it may have been exposed.
