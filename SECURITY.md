# Iron Dominion Website Security Model

## Current status

Bedrock Dominion is in development. The existing Java/Bukkit-era bridge, token plugin, and backend code are legacy references and are not approved for use with Bedrock Dominion.

## Production rules

- Never commit Discord webhook URLs, API keys, store secrets, voting private keys, or database credentials.
- Never trust browser-side actions to award tokens, verify votes, confirm purchases, or set player balances.
- Verify voting events server-side and prevent duplicate/replayed rewards before granting anything.
- Verify store-provider webhooks and entitlements server-side; never fulfill purchases from a browser redirect alone.
- Publish live server status only from a verified integration for the actual Bedrock Dominion server.
- Restrict CORS to the production website origin when the API is configured.
- Rate-limit public API endpoints and reject unexpected or oversized request bodies.
- Validate player identifiers before using them in commands, SQL, logs, or API requests.
- Use parameterized database queries and never execute arbitrary commands supplied over HTTP.
- Never expose stack traces, plugin configuration, environment variables, filesystem paths, or internal exceptions through public APIs.
- Use HTTPS, security headers, server-side secrets, backups, and a tested rollback procedure.

## Before launch

- [ ] Confirm server software and Java/Bedrock compatibility.
- [ ] Verify public domain and HTTPS configuration.
- [ ] Replace legacy status integrations and verify the new server.
- [ ] Publish actual vote listing links only after listings exist.
- [ ] Review and test any vote listener, rewards ledger, store, and delivery integration for the selected server software.
- [ ] Verify store webhook signatures and delivery/audit logs.
- [ ] Keep secrets server-side.
- [ ] Test authorization, rate limits, error handling, backups, and rollback.
- [ ] Confirm the official Discord invite.


## Repository hardening added

The repository now includes scheduled CodeQL JavaScript/TypeScript analysis, an npm dependency audit in CI, and weekly Dependabot updates for GitHub Actions and the legacy Node.js API dependencies. The additional pull-request dependency-review check requires GitHub's dependency graph to be enabled and the repository variable `DEPENDENCY_GRAPH_ENABLED=true`; until then, the npm audit job is the fallback. These checks must run successfully and their alerts must be reviewed; configuration alone does not prove that the application is secure.

The legacy Express API has also been hardened in source: proxy headers are not trusted by default, the in-memory rate-limit table is bounded, API responses are marked non-cacheable, additional defensive headers are set, SQLite foreign-key enforcement is enabled, HTTP request timeouts are configured, and balance-changing SQLite transactions are serialized to prevent concurrent requests from sharing a transaction on the single database connection. Legacy workflow tokens now default to read-only access, and the video workflow can write only when dispatched from `main`. These are code-level safeguards only. The API is still legacy reference code and is not approved for production or Bedrock Dominion.

## Account and hosting controls (manual owner actions)

These controls cannot be safely enabled through repository files alone:

- [ ] Enable phishing-resistant two-factor authentication or passkeys on the GitHub account.
- [ ] Review repository collaborators, deploy keys, GitHub Apps, OAuth grants, and personal access tokens; revoke anything unrecognized or no longer needed.
- [ ] Enable secret scanning and push protection where available; rotate any credential that was ever committed, even if the file was later removed.
- [ ] Protect `main`: require pull requests, require relevant checks, block force pushes and deletion, and limit who can bypass protections.
- [ ] Restrict deployment credentials to the minimum required scope and never place them in client-side code.
- [ ] Verify hosting-level HTTPS, security headers, access logs, backups, and rollback from the actual hosting provider. Repository configuration does not prove the live domain is receiving these settings.
- [ ] Put the Minecraft server behind provider-level DDoS protection; expose only required ports and never expose RCON or administrative interfaces publicly.
- [ ] Run a dependency audit and an authorized external security test before accepting accounts, payments, or player data.

## Incident response

If a credential may have leaked, revoke or rotate it immediately at the issuing provider, inspect recent account and deployment activity, and redeploy with the new secret. Deleting the visible file or commit does not invalidate a leaked credential. Preserve relevant logs and backups before destructive cleanup.

Do not publish vulnerability details, credentials, player information, or exploit steps in public issues. Report suspected issues privately to the repository maintainer.
