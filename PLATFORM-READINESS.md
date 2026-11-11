# Iron Dominion Platform Readiness Plan

This is the implementation checklist for turning the static website and Bedrock server into a trustworthy, repeat-visit player platform. Do not mark a capability live until it has passed an end-to-end production test.

## Release gates

### Gate 1 — Publish and verify the current account/linking work
- [ ] Confirm GitHub Pages has published the merged main branch and the canonical domain serves the latest dashboard and Minecraft-link page.
- [ ] Confirm Supabase project URL, allowed redirect URLs, email confirmation, password reset, and Microsoft OAuth callback settings.
- [ ] Confirm the minecraft_link_challenges migration is applied.
- [ ] Confirm minecraft-link Edge Function is deployed with gateway JWT verification disabled and its own user JWT checks enabled.
- [ ] Confirm DOMINION_SERVER_TOKEN exists only as a Supabase secret and protected server secret; rotate it if exposed.
- [ ] Back up the Akliz world and verify its exact Bedrock Dedicated Server build and script API support.
- [ ] Install the behavior pack on a test/backup world; test code generation, invalid/expired/reused codes, successful linking, and prevention of duplicate player identity links.
- [ ] Verify no client-side role can write trusted Minecraft identity or player stats.

### Gate 2 — Make core information trustworthy and useful
- [ ] Check every navigation link and mobile layout on the official domain.
- [ ] Ensure status is clearly sourced and stale/unavailable data is never represented as live.
- [ ] Publish accurate join instructions, supported editions/versions, rules, moderation/support path, gameplay systems, and launch state.
- [ ] Keep ranks, store items, mod packs, server addresses, rewards, and features marked unavailable until they are configured and tested.
- [ ] Add accessible, searchable guides with a last-reviewed date and clear beginner path.

### Gate 3 — Daily engagement (no fake activity or rewards)
- [ ] Choose actual public voting-listing providers and obtain the official vote URLs.
- [ ] Implement a vote history and verification flow appropriate to each provider; do not award points for an unverified click.
- [ ] Define reward currency and delivery ownership. Website-only points must not be represented as in-game currency.
- [ ] Implement daily claim/streak logic with server-side date checks, rate limits, idempotency, audit records, and recovery for failed delivery.
- [ ] Add rotating daily/weekly quests only when their completion can be verified; label manual/community quests distinctly.
- [ ] Add event calendar, announcements, changelog, community build features, and Discord entry points.

### Gate 4 — Real player profiles, statistics, and leaderboards
- [ ] Define the authoritative source for playtime, votes, economy, achievements, and event points.
- [ ] Build a trusted ingestion path from the Bedrock server; never accept player-supplied balances or statistics from browser requests.
- [ ] Add public, privacy-conscious leaderboard views with anti-abuse checks and scheduled aggregation.
- [ ] Test rankings against real server data before publishing any rows.

### Gate 5 — Store and operations
- [ ] Select a payment/store provider and confirm platform rules, taxes/refunds, privacy terms, and delivery integration.
- [ ] Use signed webhooks, idempotent order processing, reconciliation, and a support/refund workflow.
- [ ] Add uptime/error monitoring, backup/restore drills, secret rotation, dependency/security checks, and an incident rollback plan.

## Human-dependent configuration
The repository alone cannot securely configure the user's Supabase project, Akliz host, OAuth provider, voting-provider accounts, or payment account. Request only the exact action needed at each gate. Never ask the user to paste passwords, API secrets, or server tokens into chat.

## Completion definition
A page existing in GitHub is not proof a feature works. A feature is complete only when it is deployed on the official domain, tested with an ordinary player account, errors are handled, security boundaries are checked, and documentation matches actual behavior.
