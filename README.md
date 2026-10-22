# Iron Dominion — Bedrock Dominion

The public website and planning repository for **Iron Dominion** and its upcoming Minecraft server, **Bedrock Dominion**.

## Public website

- Website: https://irondominion.com/
- Server information: https://irondominion.com/join.html
- Mods and features: https://irondominion.com/mods.html
- Community Discord: https://discord.gg/bnXXc2ng72

## Current project status

Bedrock Dominion is **in development**. The website is being prepared while the server setup and feature set are designed and tested.

- Java and Bedrock support are planned; compatibility is not yet confirmed.
- The server address, port, supported versions, and launch date are not published until verified.
- Custom mods, gameplay systems, and feature details are marked **Coming Soon** until they are implemented and tested.
- Economy, ranks, store, voting rewards, leaderboards, and live server status are not advertised as active unless explicitly confirmed on the site.
- The official Discord invite above is retained for development updates.

## Website structure

- `index.html` — landing page and project announcement
- `join.html` — server connection details and launch status
- `mods.html` — custom content and feature status
- `rules.html` — community guidelines
- `ranks.html` — rank system planning
- `economy.html` — economy planning
- `leaderboard.html` — future rankings
- `shop.html` — future official store status
- `vote.html` — future voting links and rewards
- `404.html` — missing-page experience
- `css/style.css` — shared responsive visual system
- `js/server.js` — deliberately shows development status until the new server is verified
- `api/server.json` — non-live placeholder status payload

## Historical media

`assets/Iron_Dominion_OFFICIAL_TRAILER.mp4` is an older trailer for the previous server concept and is not linked from the current website. Do not use it to advertise Bedrock Dominion. The updated development-teaser workflow is manual-only and avoids claiming that unverified server features are live.

- `server/ARCHITECTURE.md` — server model decision guide and launch checklist

## Legacy integrations — not Bedrock-ready

The repository still contains earlier Java/Bukkit-era integrations under `minecraft-plugin/`, `dominion-tokens/`, `npc-plugin/`, and parts of `server/` and `api/`. These were built for an older Bukkit/Cauldron/Tekkit environment and **must not be installed on Bedrock Dominion or treated as compatible with Bedrock**.

They are retained as legacy reference code only. Before any of those systems are reused, they must be reviewed, redesigned for the selected server software, security-tested, and explicitly enabled after verification. Do not build launch instructions around the legacy artifacts.

## Security rules

- Never commit API keys, database credentials, store secrets, Discord webhooks, or server administration credentials.
- Never expose private keys or trusted API credentials in browser code.
- Do not grant purchases, ranks, votes, or rewards based on a client-side button or unverified redirect.
- Any future economy, store, voting, or player-data integration must verify requests server-side.
- Do not publish placeholder IPs, ports, fake player counts, or unverified compatibility claims.

## Development principle

**Never advertise an unfinished system as live.** Update the public site only when the corresponding Bedrock Dominion configuration has been implemented and tested.
