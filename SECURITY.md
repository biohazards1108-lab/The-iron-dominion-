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

## Donations and payment processing

- `donate.html` is informational only; no payment is accepted until official recipient details and the backend have been verified.
- Never embed PayPal client secrets, Cash App credentials, webhook secrets, or other private keys in static HTML, JavaScript served to browsers, or a public repository.
- Prefer a provider-hosted payment flow. For PayPal Checkout, create and capture orders on a trusted server and verify provider webhooks before recording payment success.
- A Cash App $cashtag transfer is not a trusted payment-verification webhook and must not automatically grant ranks, currency, or items. For automated entitlements, use an approved merchant integration with authenticated server-side confirmation.
- Use idempotency keys, amount/currency checks, order ownership checks, webhook signature verification, rate limits, audit logs, and a documented refund/dispute process before launching.
- GitHub Pages is static hosting and must not contain private payment secrets or pretend to provide a secure payment backend.
