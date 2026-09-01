// Live Minecraft 1.6.4 status endpoint for the Iron Dominion website.
// The server address is public status information; no server credentials are used.

const HOST = 'the-iron-dominion-dev.g.akliz.net';
const STATUS_URL = `https://api.mcsrvstat.us/3/${HOST}`;
const TIMEOUT_MS = 7000;

async function queryServer() {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(STATUS_URL, {
      method: 'GET',
      headers: {
        Accept: 'application/json',
        'User-Agent': 'Iron-Dominion-Website/1.0'
      },
      signal: controller.signal,
      cache: 'no-store'
    });

    if (!response.ok) throw new Error(`Status provider returned HTTP ${response.status}`);
    const data = await response.json();

    return {
      live: true,
      online: data.online === true,
      players: data.players && Number.isFinite(Number(data.players.online))
        ? Number(data.players.online)
        : null,
      maxPlayers: data.players && Number.isFinite(Number(data.players.max))
        ? Number(data.players.max)
        : null,
      version: data.version || null,
      motd: data.motd && data.motd.clean ? data.motd.clean : null,
      source: 'mcsrvstat'
    };
  } finally {
    clearTimeout(timer);
  }
}

module.exports = async function handler(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  res.setHeader('Cache-Control', 'public, max-age=20, stale-while-revalidate=40');
  res.setHeader('X-Content-Type-Options', 'nosniff');

  try {
    const result = await queryServer();
    return res.status(200).json({
      live: result.live,
      online: result.online,
      players: result.players,
      maxPlayers: result.maxPlayers,
      tps: null,
      version: result.version,
      motd: result.motd,
      updatedAt: new Date().toISOString(),
      source: result.source,
      server: HOST
    });
  } catch (error) {
    console.warn('Iron Dominion status check failed:', error);
    return res.status(200).json({
      live: false,
      online: false,
      players: null,
      maxPlayers: null,
      tps: null,
      version: null,
      motd: null,
      updatedAt: new Date().toISOString(),
      source: 'status-provider-unavailable',
      server: HOST
    });
  }
};
