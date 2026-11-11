# Daily Dominion Rewards — Deployment and Test Runbook

This runbook is for the Iron Dominion website at `https://theirondominion.com`. The daily reward feature is **not live** until every required step below is completed and verified.

## What this system awards

- 25 **website-only points** per authenticated account per UTC calendar day.
- These points are not Dominion Tokens, are not Minecraft currency, have no cash value, and cannot be spent in-game.
- The website calls the `dominion-daily-reward` Supabase Edge Function. The function validates the user's access token, derives the current date on the server in UTC, and calls a database function.
- A database unique constraint prevents a second claim for the same account and date. Only the service role can execute the award function.

## Deployment sequence

1. Review and merge the pull request after CI passes.
2. Back up the Supabase project and confirm the project URL is `https://dfdzjfsydaoovkxhilov.supabase.co`.
3. Apply `supabase/migrations/202610280004_dominion_daily_rewards.sql` to the intended Supabase project using the SQL editor or the approved migration workflow.
4. Verify both tables exist: `dominion_web_points` and `dominion_daily_claims`. Verify RLS is enabled and forced on both. Verify there are no client insert/update/delete policies and that only `service_role` can execute `claim_dominion_daily_points(uuid,date,integer)`.
5. Deploy the Edge Function from a trusted development environment with the Supabase CLI installed and authenticated:
   ```sh
   supabase link --project-ref dfdzjfsydaoovkxhilov
   supabase functions deploy dominion-daily-reward --no-verify-jwt
   ```
   `verify_jwt = false` is deliberate because the function explicitly validates the bearer token using Supabase Auth. Do not remove that in-function validation.
6. Confirm the Edge Function has its platform-provided `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` secrets. These are server-side only. Never paste the service-role key into chat, the repository, a webpage, or a browser setting.
7. Confirm Supabase Auth's Site URL and allowed redirect URLs include `https://theirondominion.com` and the required login callback URLs.
8. Wait for the GitHub Pages deployment to publish the merged website files. Do not treat a Vercel preview as production deployment.

## Release test checklist

Use a test account first; do not test by modifying another user's balance.

- [ ] Signed-out visitor sees a sign-in prompt and cannot claim.
- [ ] Signed-in account with no claim can load status.
- [ ] First claim returns `awarded: true` and a balance of 25.
- [ ] Repeating the claim returns `alreadyClaimed: true` and does not increase the balance.
- [ ] Refreshing the page still shows the same balance and claimed status.
- [ ] A second test account has a separate balance and claim record.
- [ ] A request without a valid bearer token is rejected.
- [ ] Direct browser attempts to insert or update either points table are denied by RLS/permissions.
- [ ] The page explicitly says points are website-only and not Minecraft currency.
- [ ] Browser console and Supabase function logs contain no leaked secrets or unexpected errors.
- [ ] Verify the UTC day rollover behavior in a controlled test. Do not change production balances to force a rollover test.

## Rollback

If the UI or function has a problem, first disable the claim button or redeploy the previous website version and disable the Edge Function. Preserve claim records for investigation. Do not drop tables or reset balances as a first response. If the migration needs reversal, prepare and review a separate rollback migration; do not run ad-hoc destructive SQL on production.

## Not included yet

This feature does not grant in-game currency, streak multipliers, purchasable items, vote rewards, or Minecraft server commands. Those require separately designed and tested trusted integrations.
