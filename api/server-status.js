// Vercel-safe public server-status fallback.
// The live Minecraft bridge will replace this payload once the production API
// is connected. Keeping the response explicit prevents the website from
// displaying fabricated player counts or TPS values.

module.exports = function handler(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  return res.status(200).json({
    live: false,
    online: false,
    players: null,
    maxPlayers: null,
    tps: null,
    updatedAt: null,
    source: 'vercel-fallback'
  });
};
