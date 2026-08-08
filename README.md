# Iron Dominion

Official website and integration repository for **Iron Dominion**, a Tekkit 1.6.4 survival server focused on technology, exploration, progression, trade, and player-built settlements.

## Temporary public website

GitHub Pages is the current public domain while the production `.com` domain is being prepared:

**https://biohazards1108-lab.github.io/The-iron-dominion-/**

The confirmed Discord community invite is:

**https://discord.gg/bnXXc2ng72**

When the production domain is purchased, update the public URL in deployment configuration and the sitemap; do not commit private API keys or server credentials.

## Important API hosting note

The GitHub Pages URL is the temporary **public site URL**. GitHub Pages is a static host and does **not** execute the Node.js API. The repository keeps the same URL as the temporary API reference so configuration stays consistent, but protected API endpoints remain unavailable until the Node backend is deployed to an actual server/runtime.

Do **not** point the Minecraft bridge at the GitHub Pages URL and expect `/api/*` routes to execute. Deploy the backend first, then set the real backend URL in the server-local configuration.

## Website

The public site is a static frontend suitable for GitHub Pages, Vercel, or another static host.

### Public pages

- `index.html` — landing page and server identity
- `join.html` — Tekkit 1.6.4 onboarding
- `ranks.html` — rank progression
- `economy.html` — Credits and Dominion Tokens
- `leaderboard.html` — live-data-ready leaderboard UI
- `shop.html` — professional optional-support storefront UI
- `vote.html` — voting and token-reward explanation
- `rules.html` — fair-play rules
- `404.html` — production error page

### Frontend infrastructure

- `css/style.css` — shared responsive design
- `js/server.js` — live-status client
- `js/economy.js` — economy UI helpers
- `js/leaderboard.js` — leaderboard UI helpers
- `api/server.json` — explicitly non-live placeholder feed
- `robots.txt` — crawler policy
- `sitemap.xml` — active GitHub Pages sitemap
- `.well-known/security.txt` — security contact metadata

## Security model

The browser is never trusted to award tokens, verify votes, confirm purchases, or set player balances.

The intended production flow is:

```text
Minecraft server
    ↓ authenticated bridge
Iron Dominion backend API
    ↓ HTTPS / authenticated requests
Website
```

Vote rewards follow:

```text
Vote site → Votifier-compatible receiver → server-side verification
→ duplicate/replay protection → Dominion Token grant → audit log
```

Secrets must remain server-side. Discord webhook URLs, Tebex credentials, Votifier private keys, API keys, database credentials, and Minecraft administration credentials must never be committed to this repository.

## Minecraft bridge

`minecraft-plugin/` contains the legacy Bukkit-compatible bridge intended for the Tekkit 1.6.4/Cauldron environment.

The build targets Bukkit `1.6.4-R2.0`, not modern Paper. The bridge uses HTTPS by default, an API key stored in the server-local configuration, bounded network timeouts, URL encoding, and server-side validation.

The bridge intentionally does **not** activate store item delivery by default. The legacy Tekkit item catalog must be mapped and tested before any paid product is allowed to deliver in game.

## Store

Tebex is the planned storefront provider. The public website does not embed payment forms or claim that checkout is live before a real store is configured.

## Voting

The confirmed Discord community is available at `https://discord.gg/bnXXc2ng72`.

The initial listing targets are:

- TopG
- Planet Minecraft
- Minecraft-MP

Exact Iron Dominion vote URLs will only be inserted after the server listings exist. Minecraft-MP supports Votifier and provides an API for vote-related integrations.

## Production launch checklist

The repository-side work is complete when the site is visually and structurally ready. The remaining external integration work is:

1. Purchase/register the production `.com` domain.
2. Deploy the static site over HTTPS or point the custom domain at GitHub Pages.
3. Create the actual Iron Dominion listings on the selected vote sites.
4. Configure Votifier/NuVotifier and the Dominion Token reward listener on the legacy server.
5. Deploy the backend API to a real Node.js runtime and generate a server-only API key.
6. Put the real API URL and API key into the Minecraft bridge's local `config.yml` — never GitHub.
7. Create/configure the Tebex storefront and verify legacy-server delivery.
8. Connect live server status, economy, token, and leaderboard endpoints.
9. Run a production security test and vote/store transaction test.

## Development principle

Iron Dominion follows a design-first approach: **never advertise an unfinished system as live**. Website copy should stay synchronized with the actual Minecraft server configuration.
