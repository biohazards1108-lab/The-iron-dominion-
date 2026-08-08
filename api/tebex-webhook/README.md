# Tebex webhook backend

This directory documents the production endpoint contract for the Iron Dominion Tebex integration.

## Important

GitHub Pages is static hosting and cannot execute this endpoint. The production URL must be hosted by a server/runtime that accepts HTTPS POST requests.

Recommended public endpoint:

`https://api.iron-dominion.com/api/tebex/webhook`

Do not enter that URL into Tebex until the API is actually deployed at that hostname.

## Contract

- Method: `POST`
- Content-Type: `application/json`
- Authenticate the request using Tebex's webhook signature mechanism and a secret stored only on the backend.
- Handle Tebex's validation event before processing package purchases.
- Return HTTP `2xx` only after the request is accepted safely.
- Reject invalid signatures, malformed JSON, unsupported events, and replayed/duplicate fulfillment events.
- Never grant Minecraft packages based on browser/client requests.
- Store an idempotency key/event identifier before fulfillment so Tebex retries cannot grant a package twice.

## Deployment

Deploy this contract to the chosen API host, point `api.iron-dominion.com` DNS at that service, enable HTTPS, configure the Tebex webhook secret as an environment variable, then add the exact public URL to Tebex.
