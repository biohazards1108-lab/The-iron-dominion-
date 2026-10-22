# Iron Dominion Backend — Legacy Reference

This Node.js backend was originally written for the previous Iron Dominion server integrations. It is **not a ready-to-launch backend for Bedrock Dominion**.

Do not deploy it as production infrastructure or connect it to the new server without a fresh review. Some routes and delivery workflows assume older Java/Bukkit-era systems and may not match the future Bedrock server architecture.

## Before reuse

1. Confirm the selected Bedrock server software and its supported integration model.
2. Review every route for authentication, authorization, input validation, rate limiting, and logging.
3. Replace any legacy server-status targets with a verified Bedrock Dominion status integration.
4. Keep all credentials in server-side environment variables; never place them in browser code or Git.
5. Design and test purchase fulfillment, rewards, economy changes, and audit logging before enabling them.
6. Run automated tests and security review before deployment.

## Local development

Use `npm install`, copy `.env.example` to `.env`, and configure unique random secrets locally. Never commit `.env` or a real database.

The example configuration and API documentation are development references, not confirmation that production endpoints or server integrations are live.
