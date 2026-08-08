# Iron Dominion API — Setup & Security

The API is a Node.js/Express service backed by SQLite. The production API deliberately separates public status endpoints from server/admin operations.

## 1. Requirements

- Node.js 18+ (Node 20 recommended)
- npm
- HTTPS in production
- The Iron Dominion Minecraft bridge configured with the same internal API key

## 2. Install

```bash
cd server
npm install
```

## 3. Configure secrets

Copy the example file:

```bash
cp .env.example .env
```

Generate two different random secrets. They must be at least 32 characters long:

```bash
openssl rand -hex 32
openssl rand -hex 32
```

Put the first value in `INTERNAL_API_KEY` and the second in `ADMIN_API_KEY`.

Never commit `.env`, RCON passwords, API keys, or the SQLite database.

## 4. Start

Development:

```bash
npm run dev
```

Production:

```bash
npm start
```

Syntax-only validation:

```bash
npm run check
```

## API security model

### Public

`GET /health`

Returns service health only.

`GET /api/server/status`

Returns the latest Minecraft server status. The website may consume this endpoint without an API key.

### Internal key required

Send:

```http
X-Iron-Dominion-Key: <INTERNAL_API_KEY>
```

Required for:

- `GET /api/player/:username/balance`
- `GET /api/player/:username`
- `POST /api/shop/purchase`
- `GET /api/shop/pending-deliveries`
- `POST /api/shop/deliver/:transactionId`
- `POST /api/server/update`

The browser must never contain this key. These endpoints are intended for the Minecraft bridge or another trusted server-side service.

### Admin key required

`POST /api/admin/add-tokens` requires `X-Iron-Dominion-Key` containing the `ADMIN_API_KEY`.

Do not expose this key to the public website.

## Economy protections

The purchase endpoint does **not** trust price, crate name, or item lists supplied by the client. The server owns the crate catalog.

Token deductions occur inside a SQLite `BEGIN IMMEDIATE` transaction and use a conditional balance update. This prevents concurrent requests from spending the same tokens twice or driving a balance negative.

Balances are also capped by `TOKEN_MAX_BALANCE`.

## Minecraft bridge configuration

On the Minecraft server, configure:

```yaml
api:
  url: "https://api.example.com"
  api-key: "THE_SAME_INTERNAL_API_KEY"
  connect-timeout-ms: 5000
  read-timeout-ms: 5000
  require-https: true
```

The repository's plugin configuration intentionally contains placeholders only.

## Shop delivery

The legacy Tekkit delivery processor remains disabled until the real Tekkit 1.6.4 item catalog and final store integration are verified. Pending purchases are **not** marked delivered automatically while delivery is unsupported.

## Database

The API creates the SQLite database automatically at the configured `DB_PATH`.

Runtime database files are ignored by Git. Back up the database using your hosting provider's backup system or a controlled SQLite backup process.

## Reverse proxy / HTTPS

Run the Node API behind an HTTPS reverse proxy in production. Do not expose RCON directly to the public internet.

Example Nginx configuration:

```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

Use your normal certificate automation for TLS.

## Deployment checklist

Before production:

- [ ] `.env` exists only on the server.
- [ ] `INTERNAL_API_KEY` is random and at least 32 characters.
- [ ] `ADMIN_API_KEY` is different from the internal key.
- [ ] Minecraft bridge and API use HTTPS.
- [ ] The API is behind a firewall/reverse proxy.
- [ ] RCON is not publicly exposed.
- [ ] Store delivery remains disabled until item IDs are verified.
- [ ] Database backups are configured.
- [ ] CI passes.
- [ ] API endpoints are tested with both missing and valid authentication.

## Important limitation

The website does not currently have a real player-login/authentication system. Therefore the trusted purchase and economy endpoints must not be called directly from browser JavaScript. A username typed into a public form is not proof that the requester owns that Minecraft account.

When a real authenticated web checkout is added, it should establish a server-side identity before allowing balance-changing operations.
