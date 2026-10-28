# Account platform implementation notes

## Current account behavior

- Supabase Auth supports Iron Dominion email/password accounts and Microsoft OAuth through the Azure provider.
- The static GitHub Pages site is a browser client. It must never contain a Supabase service-role key, Microsoft client secret, database password, or other trusted credential.
- The player dashboard requires a valid Supabase session and shows unavailable for game/economy fields until trusted sources exist.

## Profile schema rollout

1. Review and merge the profile/security PR.
2. In the Supabase dashboard, open SQL Editor for project `dfdzjfsydaoovkxhilov`.
3. Run `supabase/migrations/202610280002_account_profiles_and_trusted_data.sql`.
4. Confirm `profiles`, `minecraft_links`, and `player_stats` exist and RLS is enabled.
5. Test with a normal test account: it can select/update only its own profile; direct writes to `minecraft_links` and `player_stats` must be denied.
6. Sign in to the production dashboard and save a display name/username. Confirm a different user cannot read or modify that profile.
7. If the migration fails, do not partially improvise changes in production; capture the SQL error and review it before retrying.

The migration creates a default `member_<12 hex characters>` handle when a new Auth user registers. Existing Auth users who predate the trigger will not automatically get a profile from that trigger; the dashboard can create their own row through the profile insert policy when they first save.

## Minecraft ownership verification boundary

Microsoft OAuth is authentication only; it does not establish that the signed-in person owns Minecraft. Never accept a client-submitted gamertag/UUID as verified, and never accept browser-provided rank, votes, or token balances as authoritative.

A Bedrock Dedicated Server one-time-code flow has been added in `link-minecraft.html`, `supabase/functions/minecraft-link/`, and `minecraft-content/bedrock-dominion-link/`. It is not live until the new migration and Edge Function are deployed and the behavior pack is installed and tested on Akliz. Follow the **Bedrock account linking rollout** section below for the exact steps. The pack uses the pre-release `@minecraft/server-net` API, so server-build compatibility must be verified before production. The intended sequence is:
1. User starts a one-time, expiring link challenge while signed in.
2. A trusted backend creates and stores the challenge, bound to the authenticated user, with expiry and replay protection.
3. The player proves control using an official supported identity-verification mechanism or a server-side Bedrock gameplay challenge confirmed by a trusted server integration.
4. The verifier validates the proof, consumes the challenge atomically, and writes `minecraft_links` using server-only credentials.
5. The dashboard reads only the verified row under RLS.

Do not deploy an unverified self-service UUID/gamertag form as account linking. This repository is hosted on GitHub Pages; it does not run the legacy Express server, `api/*.js` handlers, or webhook code. Choose and deploy a supported backend before implementing the challenge issuer/verifier. Do not install legacy Bukkit plugins on the Bedrock server.

## Trusted ranks, votes, and Dominion Tokens

- `player_stats` is read-only for authenticated browser clients. No client-side INSERT/UPDATE/DELETE policies are provided.
- A deployed server-side integration must validate all vote provider signatures, purchases, admin grants, and game events before updating stats.
- Keep service-role credentials only in the backend secret store. Never add them to `js/`, HTML, public environment files, or Git history.
- Until the backend and source-of-truth integrations are deployed and tested, the dashboard must display `UNAVAILABLE`, not fabricated values.

## Security checks

- RLS is enabled and forced on all tables in the migration.
- Profile reads/writes are restricted to the matching `auth.uid()`.
- Trusted game tables have owner-scoped SELECT only and no client write policies.
- The profile trigger uses a fixed empty search path and is security-definer only for creating the row after Auth signup.
- A SQL migration in GitHub does not apply itself to Supabase. Production rollout requires an authorized human to run and verify it in the Supabase project.


## Bedrock account linking rollout

1. Apply `supabase/migrations/202610280003_minecraft_link_challenges.sql` in Supabase SQL Editor.
2. Deploy the Edge Function: `supabase functions deploy minecraft-link --no-verify-jwt`. It validates the user's JWT itself for website actions and requires a separate server token for in-game claims.
3. In Supabase Edge Function secrets, set `DOMINION_SERVER_TOKEN` to a randomly generated value of at least 32 characters. Do not place it in website code, GitHub, or the behavior pack files.
4. Configure the same token only in the Bedrock Dedicated Server's protected secrets file. Set the server variable `dominionLinkEndpoint` to `https://dfdzjfsydaoovkxhilov.supabase.co/functions/v1/minecraft-link`. Use the example files under `minecraft-content/bedrock-dominion-link/config/` as references; do not overwrite existing server permissions blindly.
5. Install and enable `minecraft-content/bedrock-dominion-link` as a behavior pack on a backup world first. The pack uses the pre-release `@minecraft/server-net` API documented for Bedrock Dedicated Server 1.26.60 and requires the corresponding server script permissions. Akliz's current server build/world must support this module; compatibility must be tested before production.
6. Open `https://theirondominion.com/link-minecraft.html`, generate a code while signed in, join the Bedrock server, and type `!link YOURCODE`. Refresh status on the page.
7. Confirm the link row was written by the Edge Function, the dashboard shows the verified gamertag, and direct client writes to the trusted tables remain denied.
8. If the host doesn't permit the required script API, disable the pack and use an alternate trusted server integration. Do not claim Minecraft linking is live until an end-to-end test succeeds.

Codes are ten characters, expire after ten minutes, are stored only as SHA-256 hashes, and are consumed once. The game integration proves that the user controls a website session and can submit a code from inside the server. It does not ask for or store Microsoft passwords.
