/*
 * DEPRECATED LEGACY EXAMPLE — DO NOT USE FOR BEDROCK DOMINION.
 *
 * The previous shop bridge example was not production-safe: it omitted
 * authenticated API requests, showed placeholder RCON commands, and assumed
 * Java/Bukkit-style item delivery. It has intentionally been disabled rather
 * than left as copy-paste integration code.
 *
 * Before implementing a future integration:
 * - confirm the server software and supported Bedrock APIs;
 * - authenticate every server-to-server request;
 * - never accept player balances, prices, commands, or delivery status from a browser;
 * - use server-side allowlists and parameterized database queries;
 * - keep RCON credentials and API keys out of source control;
 * - make purchase and reward delivery idempotent and auditable.
 *
 * See README.md and SECURITY.md for project constraints.
 */
'use strict';

module.exports = Object.freeze({
  status: 'deprecated',
  compatibleWithBedrockDominion: false,
  message: 'Legacy example disabled; design a verified integration for the selected server software.'
});
