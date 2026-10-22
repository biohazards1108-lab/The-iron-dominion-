# Tebex webhook — not enabled

Bedrock Dominion's public store is **not open**. The webhook source file is retained as legacy reference code only; no production endpoint is confirmed or approved for payments.

## Do not configure a live store yet

- Do not enter a webhook URL into Tebex until an API host is deployed, protected, and tested.
- Do not advertise a store or accept purchases while the website marks the store as Coming Soon.
- The current handler validates the configured signature and supports Tebex's validation handshake, but it deliberately returns HTTP 503 for `payment.completed` because durable idempotency and a verified Bedrock-compatible fulfillment bridge do not exist.
- Do not remove that safeguard until payment events are durably recorded, retries are idempotent, and fulfillment has been tested end-to-end.
- A `2xx` response for a purchase must mean the event has been durably accepted for processing, not merely logged.
- Never put webhook secrets in the website or commit them to Git.

## Requirements before activation

1. Choose and deploy the API runtime and exact public HTTPS URL.
2. Configure `TEBEX_WEBHOOK_SECRET` as a server-side secret.
3. Verify the current signature calculation against the current official Tebex documentation and test fixtures.
4. Persist webhook IDs and transaction IDs with unique constraints to prevent replay/duplicate fulfillment.
5. Verify package identifiers, player identity, payment status, and the final Bedrock server delivery mechanism.
6. Implement retries, dead-letter handling, audit logs, and support/refund procedures.
7. Run end-to-end tests before opening checkout.

Until those steps are complete, keep the store and payment fulfillment disabled.
