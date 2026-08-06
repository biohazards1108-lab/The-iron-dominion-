/**
 * Iron Dominion - Server API Backend
 * Handles player economy, shop purchases, and server data
 */

const express = require('express');
const cors = require('cors');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const app = express();
const CONFIG = require('./config.json');

// Middleware
app.use(cors());
app.use(express.json());

// Database setup
const dbPath = path.join(__dirname, '../data');
if (!fs.existsSync(dbPath)) {
  fs.mkdirSync(dbPath, { recursive: true });
}

const db = new sqlite3.Database(path.join(dbPath, 'irondom.db'), (err) => {
  if (err) {
    console.error('Database connection error:', err);
  } else {
    console.log('✓ Connected to SQLite database');
    initializeDatabase();
  }
});

// Initialize database tables
function initializeDatabase() {
  db.serialize(() => {
    // Players table
    db.run(`
      CREATE TABLE IF NOT EXISTS players (
        id INTEGER PRIMARY KEY,
        username TEXT UNIQUE NOT NULL,
        balance INTEGER DEFAULT 500,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_login DATETIME
      )
    `);

    // Transactions table
    db.run(`
      CREATE TABLE IF NOT EXISTS transactions (
        id INTEGER PRIMARY KEY,
        transaction_id TEXT UNIQUE NOT NULL,
        player_id INTEGER NOT NULL,
        username TEXT NOT NULL,
        type TEXT NOT NULL,
        amount INTEGER NOT NULL,
        description TEXT,
        status TEXT DEFAULT 'completed',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY(player_id) REFERENCES players(id)
      )
    `);

    // Shop purchases table
    db.run(`
      CREATE TABLE IF NOT EXISTS purchases (
        id INTEGER PRIMARY KEY,
        transaction_id TEXT UNIQUE NOT NULL,
        player_id INTEGER NOT NULL,
        username TEXT NOT NULL,
        crate_id TEXT NOT NULL,
        crate_name TEXT NOT NULL,
        price INTEGER NOT NULL,
        items TEXT NOT NULL,
        delivered BOOLEAN DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        delivered_at DATETIME,
        FOREIGN KEY(player_id) REFERENCES players(id)
      )
    `);

    // Server status table
    db.run(`
      CREATE TABLE IF NOT EXISTS server_status (
        id INTEGER PRIMARY KEY,
        players INTEGER,
        max_players INTEGER,
        tps REAL,
        online BOOLEAN,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `);

    console.log('✓ Database tables initialized');
  });
}

// ==================== HELPER FUNCTIONS ====================

// Generate transaction ID
function generateTransactionId() {
  return 'TXN-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
}

// Get or create player
function getOrCreatePlayer(username) {
  return new Promise((resolve, reject) => {
    db.run(
      'INSERT OR IGNORE INTO players (username) VALUES (?)',
      [username],
      function(err) {
        if (err) return reject(err);
        db.get('SELECT * FROM players WHERE username = ?', [username], (err, row) => {
          if (err) return reject(err);
          resolve(row);
        });
      }
    );
  });
}

// Get player balance
function getPlayerBalance(username) {
  return new Promise((resolve, reject) => {
    db.get('SELECT balance FROM players WHERE username = ?', [username], (err, row) => {
      if (err) return reject(err);
      resolve(row ? row.balance : 0);
    });
  });
}

// Deduct tokens from player
function deductTokens(username, amount) {
  return new Promise((resolve, reject) => {
    db.run(
      'UPDATE players SET balance = balance - ? WHERE username = ?',
      [amount, username],
      function(err) {
        if (err) return reject(err);
        resolve(this.changes > 0);
      }
    );
  });
}

// Add tokens to player
function addTokens(username, amount) {
  return new Promise((resolve, reject) => {
    db.run(
      'UPDATE players SET balance = balance + ? WHERE username = ?',
      [amount, username],
      function(err) {
        if (err) return reject(err);
        resolve(this.changes > 0);
      }
    );
  });
}

// Log transaction
function logTransaction(playerId, username, type, amount, description) {
  return new Promise((resolve, reject) => {
    const transactionId = generateTransactionId();
    db.run(
      `INSERT INTO transactions (transaction_id, player_id, username, type, amount, description)
       VALUES (?, ?, ?, ?, ?, ?)`,
      [transactionId, playerId, username, type, amount, description],
      function(err) {
        if (err) return reject(err);
        resolve(transactionId);
      }
    );
  });
}

// Log purchase
function logPurchase(playerId, username, crateId, crateName, price, items) {
  return new Promise((resolve, reject) => {
    const transactionId = generateTransactionId();
    db.run(
      `INSERT INTO purchases (transaction_id, player_id, username, crate_id, crate_name, price, items)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [transactionId, playerId, username, crateId, crateName, price, JSON.stringify(items)],
      function(err) {
        if (err) return reject(err);
        resolve(transactionId);
      }
    );
  });
}

// ==================== API ENDPOINTS ====================

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Get player balance
app.get('/api/player/:username/balance', async (req, res) => {
  try {
    const { username } = req.params;
    await getOrCreatePlayer(username);
    const balance = await getPlayerBalance(username);
    res.json({ balance });
  } catch (err) {
    console.error('Error fetching balance:', err);
    res.status(500).json({ message: 'Failed to fetch balance' });
  }
});

// Get player data (including transactions)
app.get('/api/player/:username', async (req, res) => {
  try {
    const { username } = req.params;
    const player = await getOrCreatePlayer(username);
    
    db.all(
      'SELECT * FROM transactions WHERE username = ? ORDER BY created_at DESC LIMIT 20',
      [username],
      (err, transactions) => {
        if (err) return res.status(500).json({ message: 'Failed to fetch data' });
        res.json({ player, transactions });
      }
    );
  } catch (err) {
    console.error('Error:', err);
    res.status(500).json({ message: 'Failed to fetch player data' });
  }
});

// Purchase crate
app.post('/api/shop/purchase', async (req, res) => {
  try {
    const { playerName, crateId, crateName, price, items, timestamp } = req.body;

    // Validate input
    if (!playerName || !crateId || price < 0 || !Array.isArray(items)) {
      return res.status(400).json({ message: 'Invalid purchase data' });
    }

    // Get or create player
    const player = await getOrCreatePlayer(playerName);

    // Check balance
    const balance = await getPlayerBalance(playerName);
    if (balance < price) {
      return res.status(400).json({ message: 'Insufficient Dominion Tokens. Required: ' + price + ', Available: ' + balance });
    }

    // Deduct tokens
    await deductTokens(playerName, price);

    // Log transaction
    const transactionId = await logTransaction(player.id, playerName, 'purchase', price, `Purchased ${crateName}`);

    // Log purchase
    await logPurchase(player.id, playerName, crateId, crateName, price, items);

    // Queue for delivery (in production, integrate with Minecraft server)
    console.log(`✓ Purchase completed - TXN: ${transactionId}, Player: ${playerName}, Crate: ${crateName}`);

    res.json({
      success: true,
      message: `Successfully purchased ${crateName}! Your items will arrive in your mailbox shortly.`,
      transactionId,
      newBalance: balance - price
    });
  } catch (err) {
    console.error('Purchase error:', err);
    res.status(500).json({ message: 'Purchase failed: ' + err.message });
  }
});

// Get server status
app.get('/api/server/status', (req, res) => {
  db.get(
    'SELECT * FROM server_status ORDER BY updated_at DESC LIMIT 1',
    (err, row) => {
      if (err) {
        return res.status(500).json({ message: 'Failed to fetch status' });
      }
      if (!row) {
        return res.json({ online: false, players: 0, tps: 0 });
      }
      res.json({
        online: row.online,
        players: row.players,
        maxPlayers: row.max_players,
        tps: row.tps
      });
    }
  );
});

// Update server status (called by Minecraft plugin/script)
app.post('/api/server/update', (req, res) => {
  try {
    const { players, maxPlayers, tps, online } = req.body;

    db.run(
      'INSERT INTO server_status (players, max_players, tps, online) VALUES (?, ?, ?, ?)',
      [players || 0, maxPlayers || 100, tps || 20, online !== false],
      function(err) {
        if (err) {
          console.error('Error updating status:', err);
          return res.status(500).json({ message: 'Failed to update' });
        }
        res.json({ success: true, message: 'Server status updated' });
      }
    );
  } catch (err) {
    res.status(500).json({ message: 'Update failed: ' + err.message });
  }
});

// Deliver items to player (called by scheduled job/plugin)
app.post('/api/shop/deliver/:transactionId', async (req, res) => {
  try {
    const { transactionId } = req.params;

    db.run(
      `UPDATE purchases SET delivered = 1, delivered_at = CURRENT_TIMESTAMP
       WHERE transaction_id = ?`,
      [transactionId],
      function(err) {
        if (err) {
          return res.status(500).json({ message: 'Delivery failed' });
        }
        res.json({ success: true, message: 'Items delivered' });
      }
    );
  } catch (err) {
    res.status(500).json({ message: 'Error: ' + err.message });
  }
});

// Get pending deliveries (for Minecraft plugin to process)
app.get('/api/shop/pending-deliveries', (req, res) => {
  db.all(
    `SELECT * FROM purchases WHERE delivered = 0 ORDER BY created_at ASC`,
    (err, rows) => {
      if (err) {
        return res.status(500).json({ message: 'Failed to fetch deliveries' });
      }
      // Parse items JSON
      const deliveries = rows.map(row => ({
        ...row,
        items: JSON.parse(row.items)
      }));
      res.json(deliveries);
    }
  );
});

// Add tokens (admin endpoint for rewards, etc.)
app.post('/api/admin/add-tokens', (req, res) => {
  try {
    const { playerName, amount, reason } = req.body;
    
    if (amount <= 0) {
      return res.status(400).json({ message: 'Amount must be positive' });
    }

    getOrCreatePlayer(playerName).then(player => {
      addTokens(playerName, amount);
      logTransaction(player.id, playerName, 'reward', amount, reason || 'Admin reward');
      res.json({ success: true, message: `Added ${amount} tokens to ${playerName}` });
    });
  } catch (err) {
    res.status(500).json({ message: 'Error: ' + err.message });
  }
});

// ==================== START SERVER ====================

const PORT = CONFIG.server.port || 3000;
app.listen(PORT, () => {
  console.log(`
╔════════════════════════════════════════╗
║   🏰 Iron Dominion Server API 🏰     ║
║   Running on port ${PORT}                   ║
║   Environment: ${CONFIG.server.environment}          ║
╚════════════════════════════════════════╝
  `);
});
