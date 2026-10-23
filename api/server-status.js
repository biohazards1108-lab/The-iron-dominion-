// Unavailable status endpoint for Bedrock Dominion.
// Do not poll the retired legacy Tekkit host or infer launch status from it.
module.exports = function handler(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    res.statusCode = 405;
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    return res.end(JSON.stringify({ error: 'Method not allowed' }));
  }
  res.statusCode = 200;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');
  return res.end(JSON.stringify({
    live: false,
    online: null,
    players: null,
    maxPlayers: null,
    tps: null,
    version: null,
    motd: null,
    updatedAt: null,
    source: 'unavailable',
    status: 'unavailable'
  }));
};
