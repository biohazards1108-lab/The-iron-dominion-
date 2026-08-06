# Iron Dominion Server Backend - Setup Guide

## Prerequisites

- **Node.js** v16+ ([Download](https://nodejs.org/))
- **npm** (comes with Node.js)
- Your **Minecraft server** running (for RCON integration)
- Basic knowledge of command line

---

## Installation

### 1. Install Dependencies

```bash
cd server
npm install
```

This installs:
- `express` - Web framework
- `sqlite3` - Database
- `cors` - Enable cross-origin requests
- `axios` - HTTP client for API calls
- `dotenv` - Environment variable management

### 2. Configure Environment

```bash
cp .env.example .env
nano .env
```

Edit `.env` with your server details:
```env
MC_RCON_HOST=your-server-ip-or-domain
MC_RCON_PORT=25575
MC_RCON_PASSWORD=your_secure_password
```

### 3. Start the Server

**Development** (with auto-reload):
```bash
npm run dev
```

**Production**:
```bash
npm start
```

You should see:
```
╔════════════════════════════════════════╗
║   🏰 Iron Dominion Server API 🏰     ║
║   Running on port 3000                 ║
║   Environment: production              ║
╚════════════════════════════════════════╝
```

---

## API Endpoints

### Player Economy

**Get Player Balance**
```bash
GET /api/player/{username}/balance

# Response
{
  "balance": 500
}
```

**Get Player Data**
```bash
GET /api/player/{username}

# Response
{
  "player": {
    "id": 1,
    "username": "Steve",
    "balance": 450,
    "created_at": "2026-08-06T23:13:59Z"
  },
  "transactions": [
    {
      "id": 1,
      "transaction_id": "TXN-1691358839634-abc123",
      "type": "purchase",
      "amount": 50,
      "description": "Purchased Starter Crate",
      "created_at": "2026-08-06T23:14:00Z"
    }
  ]
}
```

### Shop Purchases

**Purchase Crate**
```bash
POST /api/shop/purchase

Request Body:
{
  "playerName": "Steve",
  "crateId": "starter_crate",
  "crateName": "Starter Crate",
  "price": 100,
  "items": ["Iron Pickaxe", "Coal x64", "Wood x32"],
  "timestamp": "2026-08-06T23:14:00Z"
}

# Response
{
  "success": true,
  "message": "Successfully purchased Starter Crate! Your items will arrive in your mailbox shortly.",
  "transactionId": "TXN-1691358839634-abc123",
  "newBalance": 400
}
```

**Get Pending Deliveries** (for plugin)
```bash
GET /api/shop/pending-deliveries

# Response
[
  {
    "id": 1,
    "transaction_id": "TXN-1691358839634-abc123",
    "username": "Steve",
    "crate_id": "starter_crate",
    "crate_name": "Starter Crate",
    "price": 100,
    "items": ["Iron Pickaxe", "Coal x64", "Wood x32"],
    "delivered": 0,
    "created_at": "2026-08-06T23:14:00Z"
  }
]
```

**Mark Purchase as Delivered**
```bash
POST /api/shop/deliver/{transactionId}

# Response
{
  "success": true,
  "message": "Items delivered"
}
```

### Server Status

**Get Server Status**
```bash
GET /api/server/status

# Response
{
  "online": true,
  "players": 42,
  "maxPlayers": 100,
  "tps": 19.8
}
```

**Update Server Status** (called by plugin/script)
```bash
POST /api/server/update

Request Body:
{
  "players": 42,
  "maxPlayers": 100,
  "tps": 19.8,
  "online": true
}

# Response
{
  "success": true,
  "message": "Server status updated"
}
```

### Admin Functions

**Add Tokens to Player**
```bash
POST /api/admin/add-tokens

Request Body:
{
  "playerName": "Steve",
  "amount": 100,
  "reason": "Daily login bonus"
}

# Response
{
  "success": true,
  "message": "Added 100 tokens to Steve"
}
```

---

## Minecraft Plugin Integration

### Option 1: Java Plugin (Recommended)

Create a **Spigot/Paper plugin** that calls the API endpoints:

```java
public class IronDominionShop extends JavaPlugin {
    private String apiBase = "http://your-api-server:3000";
    
    @Override
    public void onEnable() {
        // Check for pending deliveries every 30 seconds
        getServer().getScheduler().scheduleSyncRepeatingTask(this, 
            () -> checkAndDeliverItems(), 
            0L, 600L); // 30 seconds = 600 ticks
    }
    
    private void checkAndDeliverItems() {
        // Fetch pending deliveries from API
        // Give items to players
        // Mark as delivered
    }
}
```

### Option 2: Command Block Script (Simple)

Use command blocks with `/say` to trigger deliveries:

```
/say Checking for Iron Dominion deliveries...
```

### Option 3: External Scheduler

Run a cron job or systemd timer that calls the API:

```bash
#!/bin/bash
# check-deliveries.sh
curl -X GET http://localhost:3000/api/shop/pending-deliveries | \
  jq '.[] | .transaction_id' | \
  while read txn; do
    # Process delivery
    curl -X POST http://localhost:3000/api/shop/deliver/$txn
  done
```

Add to crontab:
```bash
crontab -e
# Add: */5 * * * * /path/to/check-deliveries.sh
```

---

## Database

SQLite database automatically created at:
```
data/irondom.db
```

### View Database (Command Line)

```bash
# Install sqlite3
sqlite3 data/irondom.db

# List tables
.tables

# View players
SELECT * FROM players;

# View transactions
SELECT * FROM transactions;

# View purchases
SELECT * FROM purchases;

# Exit
.quit
```

### View Database (GUI)

Use [SQLite Browser](https://sqlitebrowser.org/) to view/edit database visually.

---

## Deployment

### Deploy to VPS (Ubuntu/Debian)

1. **Install Node.js**:
```bash
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
```

2. **Clone Repository**:
```bash
git clone https://github.com/biohazards1108-lab/The-iron-dominion-.git
cd The-iron-dominion-/server
npm install
```

3. **Configure**:
```bash
cp .env.example .env
nano .env  # Add your settings
```

4. **Use PM2 for Process Management**:
```bash
npm install -g pm2
pm2 start index.js --name "iron-dominion-api"
pm2 save
pm2 startup
```

5. **Setup Nginx Reverse Proxy**:
```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

6. **Enable SSL (Let's Encrypt)**:
```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

---

## Troubleshooting

### "Cannot find module 'express'"
```bash
npm install
```

### Port 3000 already in use
```bash
# Change port in config.json or:
PORT=3001 npm start
```

### Database locked error
- Ensure only one instance of the server is running
- Check `data/` directory permissions

### RCON connection fails
- Verify RCON is enabled on your Minecraft server
- Check `enable-rcon=true` in `server.properties`
- Verify correct password in `.env`

---

## Testing Endpoints

### Using cURL

```bash
# Check server health
curl http://localhost:3000/health

# Get player balance
curl http://localhost:3000/api/player/Steve/balance

# Test purchase
curl -X POST http://localhost:3000/api/shop/purchase \
  -H "Content-Type: application/json" \
  -d '{
    "playerName": "Steve",
    "crateId": "starter_crate",
    "crateName": "Starter Crate",
    "price": 100,
    "items": ["Iron Pickaxe", "Coal x64"]
  }'
```

### Using Postman

1. Download [Postman](https://www.postman.com/)
2. Create requests for each endpoint
3. Test and debug

---

## Support

For issues or questions:
- Check server logs: `npm start` output
- View database: `sqlite3 data/irondom.db`
- Check network: `curl http://localhost:3000/health`