# Iron Dominion

Official website and integration repository for **Iron Dominion**, a Tekkit 1.6.4 survival server focused on technology, exploration, progression, trade, and player-built settlements.

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
- `sitemap.xml` — sitemap template, to be activated once the production domain exists
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

The build targets Bukkit `1.6.4-R2.0`, not modern Paper. The bridge uses HTTPS by default, an API key stored in the server-local configuration, bounded network timeouts, URL encoding, and server-side validation. citeturn1search0

The bridge intentionally does **not** activate store item delivery by default. The legacy Tekkit item catalog must be mapped and tested before any paid product is allowed to deliver in game.

## Store

Tebex is the planned storefront provider. The public website does not embed payment forms or claim that checkout is live before a real store is configured.

## Voting

The initial listing targets are:

- TopG
- Planet Minecraft
- Minecraft-MP

Exact Iron Dominion vote URLs will only be inserted after the server listings exist. Minecraft-MP supports Votifier and provides an API for vote-related integrations. citeturn0search0turn0search6

## Production launch checklist

The repository-side work is complete when the site is visually and structurally ready. The remaining external integration work is:

1. Choose/register the production domain.
2. Deploy the static site over HTTPS.
3. Create the actual Iron Dominion listings on the selected vote sites.
4. Configure Votifier/NuVotifier and the Dominion Token reward listener on the legacy server.
5. Deploy the backend API and generate a server-only API key.
6. Put the API URL and API key into the Minecraft bridge's local `config.yml` — never GitHub.
7. Create/configure the Tebex storefront and verify legacy-server delivery.
8. Connect live server status, economy, token, and leaderboard endpoints.
9. Run a production security test and vote/store transaction test.

## Development principle

Iron Dominion follows a design-first approach: **never advertise an unfinished system as live**. Website copy should stay synchronized with the actual Minecraft server configuration.
