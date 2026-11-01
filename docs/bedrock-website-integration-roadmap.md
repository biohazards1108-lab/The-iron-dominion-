# Bedrock server ↔ Iron Dominion website integration roadmap

**Status:** planning baseline; no production behavior is changed by this document.
**Website:** https://theirondominion.com/
**Hosting:** GitHub Pages (static frontend) + Supabase (auth, database, Edge Functions)
**Minecraft host:** Akliz Bedrock Dedicated Server (verify current host panel values before deployment)

## Goals
1. Give players accurate, easy-to-copy Java/Bedrock connection instructions, with Bedrock details verified against the Akliz panel.
2. Let players create an Iron Dominion website account and optionally link a Minecraft identity.
3. Expose only trusted server data to the website (status, player count, verified profile/stat records).
4. Keep website-only Dominion points distinct from in-game items/currency until a secure, tested bridge exists.
5. Deliver changes through GitHub branches and reviewed pull requests; preserve the live site while PC-side testing is unavailable.

## Architecture
- **GitHub Pages:** public pages and browser-side UI only. Do not put secrets or server-side code here.
- **Supabase Auth:** Iron Dominion accounts; Microsoft OAuth is a separate identity-provider sign-in and must not be described as proof of Minecraft ownership.
- **Supabase Postgres:** profiles, link challenges, and trusted server-fed records protected by RLS. Browser clients must not write authoritative stats, balances, or link state.
- **Supabase Edge Functions:** authenticated challenge creation and server-authenticated claim/status ingestion. Service-role credentials remain in Supabase secrets only.
- **Bedrock server bridge:** behavior pack/script sends narrowly scoped signed/authenticated events to an Edge Function. Confirm Akliz supports the required Bedrock scripting module, network permissions, and server configuration before enabling.
- **Public status:** use an external query/status endpoint or a server-side cached relay; display “unavailable” on failure rather than inventing online status or counts.

## Player journey
1. Visit the site and read Join instructions.
2. Create/sign in to an Iron Dominion account.
3. Open Link Minecraft; request a short-lived, one-time challenge.
4. Enter the challenge in-game using the documented command, if the server bridge is installed and enabled.
5. Server validates the challenge with the Edge Function using a server-only secret; challenge is consumed atomically.
6. Website displays linked status only after server confirmation.
7. Optional trusted stats/status appear only after the server bridge is validated and data freshness is monitored.

## Security requirements
- Never collect Microsoft/Minecraft passwords; use OAuth redirects only.
- Never expose Supabase service-role keys or the server token in HTML, JavaScript, behavior-pack public files, chat, or screenshots.
- Store server token as a host-managed secret; rotate it if exposed.
- One-time challenges: cryptographically random, short expiry, single-use, rate-limited, and bound to the authenticated account.
- Server claims: authenticated, validated, replay-resistant, and idempotent.
- RLS enabled for user-owned rows; authoritative stats/balances are server-write only.
- Validate payload schema, lengths, UUID formats, and allowed event types; avoid accepting arbitrary player-supplied balances or commands.
- Add logging without tokens, passwords, or full challenge codes.
- Use least privilege and document rollback steps.

## Delivery phases
### Phase 0 — phone-friendly preparation (now)
- Audit current site pages, Supabase migrations/functions, and existing Bedrock behavior pack.
- Keep integration documentation and code in a feature branch/PR.
- Make Join page instructions clearly distinguish verified values from unavailable values.
- Review deployment prerequisites and secret names without asking the owner to share secrets.

### Phase 1 — safe public join experience
- Verify server address, port, version, and Java/Bedrock support from Akliz/server config.
- Ensure Join page has copy buttons, platform-specific instructions, troubleshooting, and honest status fallback.
- Validate public site and mobile layout.

### Phase 2 — account linking
- Review merged challenge schema/function and cooldown PR.
- Apply migrations and deploy Edge Function only after owner-controlled Supabase setup.
- Configure server secret in Akliz and install behavior pack only after host support is confirmed.
- Test challenge expiry, reuse, wrong token, cooldown, duplicate claims, and successful linking.

### Phase 3 — trusted server data
- Define exact allowlisted event schema (player UUID, event type, server timestamp, approved stat fields).
- Add server-side ingestion with idempotency and freshness metadata.
- Render stats as “unavailable” until trustworthy data arrives; never fabricate sample live data.

### Phase 4 — optional gameplay/site features
- Only after end-to-end tests: in-game reward delivery, purchases, votes, leaderboards, and any currency bridge.
- Require transaction IDs, replay protection, audit records, and explicit economy rules before money or currency features.

## PC-unavailable testing plan
**Can do now:** code review, SQL/function review, static validation, mobile page review, deployment runbook, and PR-based changes.
**Must wait for server access or a trusted test operator:** install/enable behavior pack, inspect Akliz logs/config, join from Minecraft, test command/event flow, verify actual UUIDs and server responses, and confirm Java/Bedrock connectivity.
**Release gate:** do not call the integration live until a real Bedrock join and account-link round trip succeeds and the public site reflects verified state.

## Owner-controlled deployment checklist
- [ ] Confirm current Akliz server address, port, version, and server type.
- [ ] Confirm behavior-pack/script/network permissions supported by the current Akliz Bedrock setup.
- [ ] Back up the world before installing/updating any pack.
- [ ] Apply reviewed Supabase migrations.
- [ ] Deploy reviewed Edge Functions.
- [ ] Set server-only token in host secrets; never send it in chat.
- [ ] Install pack and enable required permissions.
- [ ] Test success, expired/reused challenge, invalid token, cooldown, and server outage.
- [ ] Confirm mobile website status and linked account state.
- [ ] Publish only after the release gate passes.
