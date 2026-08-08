/**
 * Iron Dominion - Server API Backend
 *
 * Public:
 *   GET /health
 *   GET /api/server/status
 *
 * Protected by X-Iron-Dominion-Key:
 *   GET  /api/player/:username/balance
 *   GET  /api/player/:username
 *   GET  /api/shop/pending-deliveries
 *   POST /api/server/update
 *   POST /api/shop/deliver/:transactionId
 *   POST /api/shop/purchase
 *
 * Admin key required:
 *   POST /api/admin/add-tokens
 */

require('dotenv').config();

const express = require('express');
const crypto = require('crypto');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const app = express();

const PORT = Number(process.env.PORT || process.env.API_PORT || 3000);
const NODE_ENV = process.env.NODE_ENV || 'development';
const DB_PATH = path.resolve(__dirname, process.env.DB_PATH || '../data/irondom.db');
const INTERNAL_API_KEY = String(process.env.INTERNAL_API_KEY || '').trim();
const ADMIN_API_KEY = String(process.env.ADMIN_API_KEY || '').trim();
const STARTING_BALANCE = Math.max(0, Number(process.env.TOKEN_STARTING_BALANCE || 500));
const MAX_BALANCE = Math.max(STARTING_BALANCE, Number(process.env.TOKEN_MAX_BALANCE || 999999));
const SHOP_ENABLED = String(process.env.SHOP_ENABLED || 'true').toLowerCase() === 'true';
const ALLOWED_ORIGINS = String(process.env.ALLOWED_ORIGINS || '').split(',').map(v => v.trim()).filter(Boolean);

const SHOP_CRATES = Object.freeze({
  starter_crate: Object.freeze({
    name: 'Starter Crate',
    price: 100,
    items: ['Iron Pickaxe', 'Coal x64', 'Wood x32']
  }),
  industrial_crate: Object.freeze({
    name: 'Industrial Crate',
    price: 500,
    items: ['Electric Furnace', 'Engine', 'Cable x64']
  }),
  legendary_crate: Object.freeze({
    name: 'Legendary Crate',
    price: 2000,
    items: ['Quantum Armor Helmet', 'MFSU', 'Singularity']
  })
});

if (!Number.isInteger(PORT) || PORT < 1 || PORT > 65535) {
  throw new Error('PORT/API_PORT must be a valid TCP port');
}
if (!INTERNAL_API_KEY || INTERNAL_API_KEY.length < 32) {
  console.warn('WARNING: INTERNAL_API_KEY is missing or shorter than 32 characters. Protected API calls will be rejected.');
}
if (!ADMIN_API_KEY || ADMIN_API_KEY.length < 32) {
  console.warn('WARNING: ADMIN_API_KEY is missing or shorter than 32 characters. Admin calls will be rejected.');
}

fs.mkdirSync(path.dirname(DB_PATH), { recursive: true });

// -------------------- HTTP SECURITY --------------------
app.disable('x-powered-by');
app.set('trust proxy', 1);

app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
  if (req.secure || req.headers['x-forwarded-proto'] === 'https') {
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  }
  next();
});

app.use((req, res, next) => {
  const origin = req.headers.origin;
  if (!origin) return next();
  if (ALLOWED_ORIGINS.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
    res.setHeader('Vary', 'Origin');
    res.setHeader('Access-Control-Allow-Credentials', 'false');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Iron-Dominion-Key');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    if (req.method === 'OPTIONS') return res.status(204).end();
    return next();
  }
  return res.status(403).json({ message: 'Origin not allowed' });
});

app.use(express.json({ limit: '16kb', strict: true }));

// Small in-memory rate limiter. This is intentionally simple and bounded; a
// reverse proxy/WAF should provide the production-grade distributed limit.
const rateBuckets = new Map();
const RATE_WINDOW_MS = 60 * 1000;
const RATE_LIMIT = 120;
setInterval(() => {
  const cutoff = Date.now() - RATE_WINDOW_MS;
  for (const [key, bucket] of rateBuckets) {
    if (bucket.started < cutoff) rateBuckets.delete(key);
  }
}, RATE_WINDOW_MS).unref();

function rateLimit(req, res, next) {
  const key = String(req.ip || req.socket.remoteAddress || 'unknown');
  const now = Date.now();
  let bucket = rateBuckets.get(key);
  if (!bucket || now - bucket.started >= RATE_WINDOW_MS) {
    bucket = { started: now, count: 0 };
    rateBuckets.set(key, bucket);
  }
  bucket.count += 1;
  res.setHeader('X-RateLimit-Limit', RATE_LIMIT);
  res.setHeader('X-RateLimit-Remaining', Math.max(0, RATE_LIMIT - bucket.count));
  if (bucket.count > RATE_LIMIT) {
    return res.status(429).json({ message: 'Too many requests. Please try again later.' });
  }
  next();
}
app.use(rateLimit);

// -------------------- DATABASE --------------------
const db = new sqlite3.Database(DB_PATH, err => {
  if (err) {
    console.error('Database connection error:', err);
    process.exitCode = 1;
    return;
  }
  initializeDatabase();
});

db.configure('busyTimeout', 5000);

function run(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.run(sql, params, function(err) {
      if (err) return reject(err);
      resolve({ changes: this.changes, lastID: this.lastID });
    });
  });
}

function get(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => err ? reject(err) : resolve(row));
  });
}

function all(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => err ? reject(err) : resolve(rows));
  });
}

function initializeDatabase() {
  db.serialize(() => {
    db.run(`CREATE TABLE IF NOT EXISTS players (
      id INTEGER PRIMARY KEY,
      username TEXT UNIQUE NOT NULL COLLATE NOCASE,
      balance INTEGER NOT NULL DEFAULT 500 CHECK(balance >= 0),
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      last_login DATETIME
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY,
      transaction_id TEXT UNIQUE NOT NULL,
      player_id INTEGER NOT NULL,
      username TEXT NOT NULL,
      type TEXT NOT NULL CHECK(type IN ('purchase', 'reward')),
      amount INTEGER NOT NULL,
      description TEXT,
      status TEXT DEFAULT 'completed',
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(player_id) REFERENCES players(id)
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS purchases (
      id INTEGER PRIMARY KEY,
      transaction_id TEXT UNIQUE NOT NULL,
      player_id INTEGER NOT NULL,
      username TEXT NOT NULL,
      crate_id TEXT NOT NULL,
      crate_name TEXT NOT NULL,
      price INTEGER NOT NULL CHECK(price >= 0),
      items TEXT NOT NULL,
      delivered BOOLEAN DEFAULT 0,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      delivered_at DATETIME,
      FOREIGN KEY(player_id) REFERENCES players(id)
    )`);

    db.run(`CREATE TABLE IF NOT EXISTS server_status (
      id INTEGER PRIMARY KEY,
      players INTEGER NOT NULL,
      max_players INTEGER NOT NULL,
      tps REAL NOT NULL,
      online BOOLEAN NOT NULL,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    db.run('CREATE INDEX IF NOT EXISTS idx_transactions_username ON transactions(username, created_at DESC)');
    db.run('CREATE INDEX IF NOT EXISTS idx_purchases_delivered ON purchases(delivered, created_at ASC)');
    db.run('CREATE INDEX IF NOT EXISTS idx_server_status_updated ON server_status(updated_at DESC)');

    console.log(`✓ SQLite database ready: ${DB_PATH}`);
  });
}

// -------------------- VALIDATION / AUTH --------------------
function normalizeUsername(value) {
  if (typeof value !== 'string') return null;
  const username = value.trim();
  // Minecraft usernames are 3-16 ASCII letters, digits and underscores.
  return /^[A-Za-z0-9_]{3,16}$/.test(username) ? username : null;
}

function normalizeTransactionId(value) {
  if (typeof value !== 'string') return null;
  const id = value.trim();
  return /^TXN-[A-Za-z0-9_-]{8,80}$/.test(id) ? id : null;
}

function positiveInteger(value, max = MAX_BALANCE) {
  if (!Number.isInteger(value) || value <= 0 || value > max) return null;
  return value;
}

function safeEqual(a, b) {
  const left = Buffer.from(String(a || ''), 'utf8');
  const right = Buffer.from(String(b || ''), 'utf8');
  return left.length === right.length && crypto.timingSafeEqual(left, right);
}

function requireInternalKey(req, res, next) {
  const supplied = req.get('X-Iron-Dominion-Key');
  if (!INTERNAL_API_KEY || !supplied || !safeEqual(supplied, INTERNAL_API_KEY)) {
    return res.status(401).json({ message: 'Authentication required' });
  }
  next();
}

function requireAdminKey(req, res, next) {
  const supplied = req.get('X-Iron-Dominion-Key');
  if (!ADMIN_API_KEY || !supplied || !safeEqual(supplied, ADMIN_API_KEY)) {
    return res.status(403).json({ message: 'Administrator authentication required' });
  }
  next();
}

function generateTransactionId() {
  return `TXN-${Date.now()}-${crypto.randomBytes(6).toString('hex')}`;
}

async function getOrCreatePlayer(username) {
  await run(
    'INSERT OR IGNORE INTO players (username, balance) VALUES (?, ?)',
    [username, STARTING_BALANCE]
  );
  return get('SELECT id, username, balance, created_at, last_login FROM players WHERE username = ?', [username]);
}

async function recordTransaction(playerId, username, type, amount, description, transactionId) {
  await run(
    `INSERT INTO transactions (transaction_id, player_id, username, type, amount, description)
     VALUES (?, ?, ?, ?, ?, ?)`,
    [transactionId, playerId, username, type, amount, description]
  );
}

// -------------------- PUBLIC ENDPOINTS --------------------
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'iron-dominion-api', timestamp: new Date().toISOString() });
});

app.get('/api/server/status', async (req, res) => {
  try {
    const row = await get('SELECT * FROM server_status ORDER BY updated_at DESC LIMIT 1');
    if (!row) return res.json({ live: false, online: false, players: null, maxPlayers: null, tps: null, updatedAt: null });
    res.json({
      live: true,
      online: Boolean(row.online),
      players: row.players,
      maxPlayers: row.max_players,
      tps: row.tps,
      updatedAt: row.updated_at
    });
  } catch (err) {
    console.error('Status read error:', err);
    res.status(500).json({ message: 'Failed to fetch server status' });
  }
});

// -------------------- PROTECTED PLAYER ENDPOINTS --------------------
app.get('/api/player/:username/balance', requireInternalKey, async (req, res) => {
  try {
    const username = normalizeUsername(req.params.username);
    if (!username) return res.status(400).json({ message: 'Invalid Minecraft username' });
    const player = await getOrCreatePlayer(username);
    res.json({ username: player.username, balance: player.balance });
  } catch (err) {
    console.error('Balance error:', err);
    res.status(500).json({ message: 'Failed to fetch balance' });
  }
});

app.get('/api/player/:username', requireInternalKey, async (req, res) => {
  try {
    const username = normalizeUsername(req.params.username);
    if (!username) return res.status(400).json({ message: 'Invalid Minecraft username' });
    const player = await getOrCreatePlayer(username);
    const transactions = await all(
      `SELECT transaction_id, type, amount, description, status, created_at
       FROM transactions WHERE username = ? ORDER BY created_at DESC LIMIT 20`,
      [username]
    );
    res.json({ player, transactions });
  } catch (err) {
    console.error('Player data error:', err);
    res.status(500).json({ message: 'Failed to fetch player data' });
  }
});

// -------------------- SHOP --------------------
app.post('/api/shop/purchase', requireInternalKey, async (req, res) => {
  if (!SHOP_ENABLED) return res.status(503).json({ message: 'The shop is temporarily disabled' });

  const username = normalizeUsername(req.body && req.body.playerName);
  const crateId = typeof (req.body && req.body.crateId) === 'string' ? req.body.crateId.trim() : '';
  const crate = SHOP_CRATES[crateId];

  if (!username || !crate) return res.status(400).json({ message: 'Invalid purchase request' });

  const player = await getOrCreatePlayer(username);
  const transactionId = generateTransactionId();

  try {
    await run('BEGIN IMMEDIATE TRANSACTION');

    // The database performs the balance check and deduction atomically. The
    // client cannot choose a price, item list, or crate name.
    const deduction = await run(
      'UPDATE players SET balance = balance - ? WHERE id = ? AND balance >= ? AND balance - ? <= ?',
      [crate.price, player.id, crate.price, crate.price, MAX_BALANCE]
    );

    if (deduction.changes !== 1) {
      await run('ROLLBACK');
      const current = await get('SELECT balance FROM players WHERE id = ?', [player.id]);
      return res.status(400).json({
        message: `Insufficient Dominion Tokens. Required: ${crate.price}, Available: ${current ? current.balance : 0}`
      });
    }

    await recordTransaction(player.id, player.username, 'purchase', -crate.price, `Purchased ${crate.name}`, transactionId);
    await run(
      `INSERT INTO purchases (transaction_id, player_id, username, crate_id, crate_name, price, items)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [transactionId, player.id, player.username, crateId, crate.name, crate.price, JSON.stringify(crate.items)]
    );

    await run('COMMIT');

    const updated = await get('SELECT balance FROM players WHERE id = ?', [player.id]);
    res.status(201).json({
      success: true,
      message: `Successfully purchased ${crate.name}. Your items are queued for delivery.`,
      transactionId,
      newBalance: updated.balance
    });
  } catch (err) {
    try { await run('ROLLBACK'); } catch (_) { /* transaction may already be closed */ }
    console.error('Purchase error:', err);
    res.status(500).json({ message: 'Purchase failed' });
  }
});

app.get('/api/shop/pending-deliveries', requireInternalKey, async (req, res) => {
  try {
    const rows = await all(
      `SELECT transaction_id, player_id, username, crate_id, crate_name, price, items, created_at
       FROM purchases WHERE delivered = 0 ORDER BY created_at ASC LIMIT 100`,
      []
    );
    const deliveries = rows.map(row => ({
      ...row,
      items: JSON.parse(row.items)
    }));
    res.json(deliveries);
  } catch (err) {
    console.error('Pending delivery error:', err);
    res.status(500).json({ message: 'Failed to fetch deliveries' });
  }
});

app.post('/api/shop/deliver/:transactionId', requireInternalKey, async (req, res) => {
  const transactionId = normalizeTransactionId(req.params.transactionId);
  if (!transactionId) return res.status(400).json({ message: 'Invalid transaction ID' });

  try {
    const result = await run(
      `UPDATE purchases SET delivered = 1, delivered_at = CURRENT_TIMESTAMP
       WHERE transaction_id = ? AND delivered = 0`,
      [transactionId]
    );
    if (result.changes !== 1) {
      return res.status(404).json({ message: 'Pending transaction not found' });
    }
    res.json({ success: true, message: 'Items marked as delivered' });
  } catch (err) {
    console.error('Delivery error:', err);
    res.status(500).json({ message: 'Delivery failed' });
  }
});

// -------------------- SERVER BRIDGE --------------------
app.post('/api/server/update', requireInternalKey, async (req, res) => {
  const players = Number(req.body && req.body.players);
  const maxPlayers = Number(req.body && req.body.maxPlayers);
  const tps = Number(req.body && req.body.tps);
  const online = req.body && req.body.online;

  if (!Number.isInteger(players) || players < 0 || players > 100000 ||
      !Number.isInteger(maxPlayers) || maxPlayers < 1 || maxPlayers > 100000 ||
      players > maxPlayers || !Number.isFinite(tps) || tps < 0 || tps > 100 ||
      typeof online !== 'boolean') {
    return res.status(400).json({ message: 'Invalid server status data' });
  }

  try {
    await run(
      'INSERT INTO server_status (players, max_players, tps, online) VALUES (?, ?, ?, ?)',
      [players, maxPlayers, tps, online ? 1 : 0]
    );
    res.status(201).json({ success: true });
  } catch (err) {
    console.error('Status update error:', err);
    res.status(500).json({ message: 'Failed to update server status' });
  }
});

// -------------------- ADMIN --------------------
app.post('/api/admin/add-tokens', requireAdminKey, async (req, res) => {
  const username = normalizeUsername(req.body && req.body.playerName);
  const amount = positiveInteger(req.body && req.body.amount, 1000000);
  const reason = typeof (req.body && req.body.reason) === 'string'
    ? req.body.reason.trim().slice(0, 200)
    : 'Admin reward';

  if (!username || !amount) return res.status(400).json({ message: 'Invalid token grant' });

  try {
    const player = await getOrCreatePlayer(username);
    await run('BEGIN IMMEDIATE TRANSACTION');
    const result = await run(
      'UPDATE players SET balance = balance + ? WHERE id = ? AND balance + ? <= ?',
      [amount, player.id, amount, MAX_BALANCE]
    );

    if (result.changes !== 1) {
      await run('ROLLBACK');
      return res.status(409).json({ message: 'Token grant would exceed the maximum balance' });
    }

    const transactionId = generateTransactionId();
    await recordTransaction(player.id, player.username, 'reward', amount, reason, transactionId);
    await run('COMMIT');

    const updated = await get('SELECT balance FROM players WHERE id = ?', [player.id]);
    res.status(201).json({ success: true, transactionId, username: player.username, balance: updated.balance });
  } catch (err) {
    try { await run('ROLLBACK'); } catch (_) { /* transaction may already be closed */ }
    console.error('Admin token error:', err);
    res.status(500).json({ message: 'Token grant failed' });
  }
});

// -------------------- ERROR HANDLING --------------------
app.use((req, res) => res.status(404).json({ message: 'Endpoint not found' }));
app.use((err, req, res, next) => {
  console.error('Unhandled API error:', err);
  if (res.headersSent) return next(err);
  if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
    return res.status(400).json({ message: 'Invalid JSON body' });
  }
  res.status(500).json({ message: 'Internal server error' });
});

const server = app.listen(PORT, () => {
  console.log(`Iron Dominion API listening on port ${PORT} (${NODE_ENV})`);
});

function shutdown(signal) {
  console.log(`${signal}: shutting down...`);
  server.close(() => {
    db.close(() => process.exit(0));
  });
  setTimeout(() => process.exit(1), 10000).unref();
}
process.on('SIGINT', () => shutdown('SIGINT'));
process.on('SIGTERM', () => shutdown('SIGTERM'));
