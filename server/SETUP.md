# Backend Setup — Legacy Reference Only

This guide describes the existing Node.js backend, which was created for earlier Iron Dominion integrations. It is **not production-ready for Bedrock Dominion** and must not be connected to the new server without review.

## Local-only setup

- Node.js 18+ (Node 20 recommended)
- npm
- A local `.env` copied from `.env.example`

Never commit real credentials, API keys, RCON passwords, database files, or store secrets.

## Required review before any deployment

- Confirm Bedrock Dominion's final server software and integration approach.
- Audit all routes for authentication, authorization, input validation, rate limiting, and safe error handling.
- Remove legacy hostnames and test status queries against the actual new server only after its address is confirmed.
- Test all economy, purchase, delivery, voting, and administrative actions with server-side verification.
- Confirm that no browser-facing endpoint can change balances or grant items without trusted authorization.
- Configure HTTPS, firewall rules, logs, backups, and a rollback plan.

## Important limitations

The static website does not itself provide a trusted player-login system. A username typed into a form is not proof of Minecraft account ownership. Never expose internal/admin API keys to browser JavaScript.

Legacy Java/Bukkit/Tekkit assumptions in this repository do not establish Bedrock compatibility. Keep unfinished systems disabled until they are deliberately rebuilt and tested.
