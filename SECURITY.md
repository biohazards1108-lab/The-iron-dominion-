# Iron Dominion Website Security Model

## Production rules

- Never place Discord webhook URLs, API keys, store secrets, Votifier private keys, or database credentials in frontend HTML/JavaScript.
- The browser must never be trusted to award Dominion Tokens, confirm votes, confirm purchases, or set player balances.
- Vote events must be verified server-side and deduplicated before rewards are granted.
- Payment fulfillment must be verified by the store provider/server integration rather than a browser redirect.
- Live server status should be read from a controlled backend endpoint, not from client-supplied data.
- CORS should be restricted to the final production website origin once a domain exists.
- Rate-limit public API endpoints and reject oversized/unexpected request bodies.
- Validate Minecraft usernames server-side before using them in commands, SQL, logs, or API requests.
- Use parameterized database queries if a database is introduced.
- Never execute arbitrary commands supplied by HTTP requests.
- Never expose stack traces, plugin configuration, environment variables, filesystem paths, or internal exception details through the public API.
- Use HTTPS in production and security headers including Content-Security-Policy, Referrer-Policy, X-Content-Type-Options, and frame protections.
- Keep server-side secrets in environment variables or the hosting provider's secret store.

## Voting architecture

`Voting Site -> Votifier/NuVotifier -> Server Vote Listener -> Verified Reward -> Dominion Token Ledger`

The website is informational and should link players to the official listing vote URLs. It must not accept a client-side "vote completed" request as proof.

NuVotifier documentation notes that its public/private keys and service tokens are security-sensitive and that vote listeners are responsible for processing rewards. Final legacy-server plugin versions must therefore be compatibility-tested before production use.

## Store architecture

`Player -> Store Provider Checkout -> Provider Verification/Webhook -> Server Delivery -> Audit Log`

Do not put a store API secret in static website code. Do not grant ranks, tokens, or other products merely because a user returns from a checkout page.

## Launch checklist

- [ ] Final website domain selected
- [ ] HTTPS enabled
- [ ] Final server-status backend connected
- [ ] Final vote listings published
- [ ] NuVotifier/Votifier compatibility tested on Tekkit 1.6.4/Cauldron
- [ ] Token ledger/reward listener tested for duplicate and replayed votes
- [ ] Store provider compatibility tested on the legacy server
- [ ] Store webhook/signature verification tested
- [ ] Discord public invite verified
- [ ] Discord webhooks kept server-side
- [ ] Security headers verified
- [ ] Public API rate limits verified
- [ ] Error responses checked for information leakage
- [ ] Backup and rollback procedure tested
