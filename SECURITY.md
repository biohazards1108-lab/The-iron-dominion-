# Iron Dominion Website Security

## Scope
This repository contains the public website. Secrets, Discord webhooks, payment credentials, Minecraft RCON credentials, database credentials, and vote-provider credentials must never be committed here.

## Security architecture
- Browser code must treat all client input as untrusted.
- Vote rewards must be verified and granted server-side.
- Token balances must never be accepted from browser requests.
- Administrative endpoints must require authentication and authorization.
- CORS should allow only the production website origin for authenticated API calls.
- API responses should not expose credentials, player PII, session tokens, or internal server paths.
- Rate-limit public endpoints, especially vote/status endpoints.
- Validate and bound all numeric values received by APIs.
- Use HTTPS in production.
- Keep secrets in environment variables or the hosting provider's secret store.

## Reporting
Replace the contact placeholder in `security.txt` before public launch.
