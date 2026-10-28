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

A production link flow still requires a trusted verifier and game integration. The intended sequence is:
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
