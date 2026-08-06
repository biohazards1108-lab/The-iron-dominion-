# Iron Dominion Server Backend

API server for managing player economy, shop purchases, and server data for the Iron Dominion Minecraft server.

## Quick Start

### Installation

```bash
cd server
npm install
cp .env.example .env
```

### Configuration

Edit `.env` with your server details:
```env
MC_RCON_HOST=your-server-ip
MC_RCON_PORT=25575
MC_RCON_PASSWORD=your_password
```

### Run

**Development:**
```bash
npm run dev
```

**Production:**
```bash
npm start
```

Server runs on `http://localhost:3000`

---

## Features

✅ Player token economy system  
✅ Shop system with crate purchases  
✅ Automatic item delivery to players  
✅ Server status monitoring  
✅ Transaction logging  
✅ Admin reward system  

---

## API Endpoints

### Player API
- `GET /api/player/:username/balance` - Get player balance
- `GET /api/player/:username` - Get player data and history

### Shop API
- `POST /api/shop/purchase` - Purchase a crate
- `GET /api/shop/pending-deliveries` - Get items to deliver
- `POST /api/shop/deliver/:transactionId` - Mark as delivered

### Server API
- `GET /api/server/status` - Get server status
- `POST /api/server/update` - Update server stats

### Admin API
- `POST /api/admin/add-tokens` - Add tokens to player

---

## Database

SQLite database with tables:
- `players` - Player accounts and balances
- `transactions` - Token transactions
- `purchases` - Shop purchases
- `server_status` - Server metrics

---

## Minecraft Integration

See `minecraft-plugin-example.js` for how to integrate with your Minecraft server.

### Key Integration Points

1. **Pending Deliveries**: Poll `/api/shop/pending-deliveries` every 30 seconds
2. **Item Delivery**: Use RCON or direct database to give items to players
3. **Mark Delivered**: Call `/api/shop/deliver/:transactionId` when done
4. **Server Status**: Post to `/api/server/update` with current server stats

---

## Deployment

See `SETUP.md` for detailed deployment instructions for:
- Local development
- VPS deployment (Ubuntu/Debian)
- Using PM2 for process management
- Nginx reverse proxy setup
- SSL/TLS with Let's Encrypt

---

## File Structure

```
server/
├── index.js                      # Main server
├── config.json                   # Configuration
├── package.json                  # Dependencies
├── .env.example                  # Environment template
├── minecraft-plugin-example.js   # Plugin integration guide
├── SETUP.md                      # Detailed setup guide
└── README.md                     # This file
```

---

## Contributing

Contributions welcome! Feel free to submit issues and pull requests.

---

## License

Iron Dominion © 2026