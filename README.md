# Iron Dominion — Bedrock Dominion

The public website and development repository for **Iron Dominion** and **Bedrock Dominion**.

## Public website

- Website: https://theirondominion.com/
- Server information: https://theirondominion.com/join.html
- Microsoft/Minecraft account shortcuts: https://theirondominion.com/account.html
- Mods and features: https://theirondominion.com/mods.html
- Player guides: https://theirondominion.com/guides.html
- Ranks: https://theirondominion.com/ranks.html
- Currency: https://theirondominion.com/currency.html
- Economy: https://theirondominion.com/economy.html
- Store: https://theirondominion.com/shop.html
- Server status: https://theirondominion.com/status.html
- FAQ: https://theirondominion.com/faq.html
- Support: https://theirondominion.com/support.html
- Rules: https://theirondominion.com/rules.html
- Privacy: https://theirondominion.com/privacy.html
- Terms: https://theirondominion.com/terms.html
- Development log: https://theirondominion.com/changelog.html
- Community Discord: https://discord.gg/bnXXc2ng72

## Current project status

Bedrock Dominion's Akliz-hosted native Bedrock server has been started, and the owner has successfully connected and moved around in-game. The website and custom gameplay systems remain in active development.

- Java and Bedrock support are planned; compatibility is not yet confirmed.
- The server address, port, supported versions, and launch date are not published until verified.
- Custom mods, gameplay systems, and feature details are marked **Coming Soon** until they are implemented and tested.
- Economy, ranks, store, voting rewards, leaderboards, and live server status are not advertised as active unless explicitly confirmed on the site.
- The official Discord invite above is retained for development updates.

## Website structure

- `index.html` — landing page and project announcement
- `join.html` — server connection details and launch status
- `account.html` — shortcuts to official Microsoft/Minecraft account portals (not an in-site login)
- `mods.html` — custom content and feature status
- `guides.html` — player preparation and future how-to guides
- `rules.html` — community guidelines
- `ranks.html` — rank system planning
- `economy.html` — economy planning
- `currency.html` — currency status and future currency guide
- `leaderboard.html` — future rankings
- `shop.html` — future official store status
- `vote.html` — future voting links and rewards
- `status.html` — external Bedrock status-provider check; its response may differ from actual player connectivity and is not authoritative when it cannot detect the server
- `faq.html` — frequently asked questions
- `support.html` — support and safety guidance
- `privacy.html` — website privacy notice
- `terms.html` — website terms of use
- `changelog.html` — development changelog
- `404.html` — missing-page experience
- `css/style.css` — shared responsive visual system
- `js/server.js` — deliberately shows development status until the new server is verified
- `api/server.json` — non-live placeholder status payload

## Hosting and API boundary

The public site is hosted as a static GitHub Pages website. GitHub Pages serves HTML, CSS, JavaScript, and static JSON files; it does **not** execute the repository's `api/*.js` functions, the legacy Express server, or webhook handlers. `vercel.json` does not configure GitHub Pages. Therefore, API and payment integrations must not be described as live based on files existing in this repository. A separate trusted backend and explicit deployment are required before live status, Microsoft OAuth, purchases, voting rewards, economy, or player data can work.

## Server planning

- `server/ARCHITECTURE.md` — server model decision guide, trade-offs, security baseline, and launch checklist

## Historical media

`assets/Iron_Dominion_OFFICIAL_TRAILER.mp4` is an older trailer for the previous server concept and is not linked from the current website. Do not use it to advertise Bedrock Dominion. The updated development-teaser workflow is manual-only and avoids claiming that unverified server features are live.

## Legacy integrations — not Bedrock-ready

The repository still contains earlier Java/Bukkit-era integrations under `minecraft-plugin/`, `dominion-tokens/`, `npc-plugin/`, and parts of `server/` and `api/`. These were built for an older Bukkit/Cauldron/Tekkit environment and **must not be installed on Bedrock Dominion or treated as compatible with Bedrock**.

They are retained as legacy reference code only. Before any of those systems are reused, they must be reviewed, redesigned for the selected server software, security-tested, and explicitly enabled after verification. Do not build launch instructions around the legacy artifacts.

## Security rules

- Never commit API keys, database credentials, store secrets, Discord webhooks, or server administration credentials.
- Never expose private keys or trusted API credentials in browser code.
- Do not grant purchases, ranks, votes, or rewards based on a client-side button or unverified redirect.
- Any future economy, store, voting, or player-data integration must verify requests server-side.
- Do not publish placeholder IPs, ports, fake player counts, or unverified compatibility claims.

## Automated quality and security checks

GitHub Actions validates public-page links and assets, keyboard skip links, canonical metadata, the full sitemap, safe external links, the static-site Content Security Policy, social preview metadata, development-status wording, and accidental secret patterns. A separate workflow runs CodeQL JavaScript analysis and an npm dependency audit; Dependabot proposes dependency updates. These checks reduce risk but do not guarantee the site is vulnerability-free. The Content Security Policy is delivered as a meta policy because GitHub Pages does not let this repository configure arbitrary HTTP response headers.

## Development principle

**Never advertise an unfinished system as live.** Update the public site only when the corresponding Bedrock Dominion configuration has been implemented and tested.
