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
    online: 'unavailable',
    players: 'unavailable',
    maxPlayers: 'unavailable',
    tps: 'unavailable',
    version: 'unavailable',
    motd: 'unavailable',
    updatedAt: 'unavailable',
    source: 'unavailable',
    status: 'unavailable'
  }));
};
