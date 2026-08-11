// Live Minecraft 1.6.4 status endpoint using the legacy server-list ping.
// This does not expose server credentials; it only queries public status.

const HOST = 'the-iron-dominion-dev.g.akliz.net';
const PORT = 25565;
const TIMEOUT_MS = 5000;

function parseLegacyResponse(buffer) {
  // Legacy ping response: FF + UTF-16BE payload beginning with §1.
  if (!buffer || buffer.length < 3 || buffer[0] !== 0xff) return null;
  const payload = buffer.subarray(3).toString('utf16le').replace(/\u0000/g, '');
  const parts = payload.split('\u0000');
  if (parts.length < 6) return null;

  // Depending on Node/version and server implementation, the UTF-16 byte order
  // may arrive reversed; fall back to decoding the raw bytes as BE code units.
  return parts;
}

function decodePayload(buffer) {
  if (!buffer || buffer[0] !== 0xff) return null;
  const bytes = buffer.subarray(3);
  let text = '';
  for (let i = 0; i + 1 < bytes.length; i += 2) {
    text += String.fromCharCode((bytes[i] << 8) | bytes[i + 1]);
  }
  return text.replace(/\u0000/g, '').split('\u0000');
}

function queryServer() {
  return new Promise((resolve) => {
    const net = require('net');
    const socket = new net.Socket();
    const chunks = [];
    let settled = false;

    const finish = (result) => {
      if (settled) return;
      settled = true;
      socket.destroy();
      resolve(result);
    };

    const timer = setTimeout(() => finish(null), TIMEOUT_MS);

    socket.once('error', () => {
      clearTimeout(timer);
      finish(null);
    });

    socket.once('close', () => {
      clearTimeout(timer);
      if (!settled && chunks.length) {
        const parsed = decodePayload(Buffer.concat(chunks));
        if (parsed && parsed.length >= 6 && parsed[0] === '§1') {
          const online = Number(parsed[4]);
          const maxPlayers = Number(parsed[5]);
          finish({
            live: true,
            online: Number.isFinite(online),
            players: Number.isFinite(online) ? online : null,
            maxPlayers: Number.isFinite(maxPlayers) ? maxPlayers : null
          });
        } else {
          finish(null);
        }
      }
    });

    socket.on('data', (chunk) => {
      chunks.push(chunk);
      // A legacy ping response is small; process it as soon as it arrives.
      const parsed = decodePayload(Buffer.concat(chunks));
      if (parsed && parsed.length >= 6 && parsed[0] === '§1') {
        clearTimeout(timer);
        const online = Number(parsed[4]);
        const maxPlayers = Number(parsed[5]);
        finish({
          live: true,
          online: Number.isFinite(online) ? online >= 0 : false,
          players: Number.isFinite(online) ? online : null,
          maxPlayers: Number.isFinite(maxPlayers) ? maxPlayers : null
        });
      }
    });

    socket.connect(PORT, HOST, () => {
      socket.write(Buffer.from([0xfe, 0x01]));
    });
  });
}

module.exports = async function handler(req, res) {
  if (req.method !== 'GET') {
    res.setHeader('Allow', 'GET');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Content-Type-Options', 'nosniff');

  const result = await queryServer();
  return res.status(200).json({
    live: Boolean(result),
    online: result ? Boolean(result.online) : false,
    players: result ? result.players : null,
    maxPlayers: result ? result.maxPlayers : null,
    tps: null,
    updatedAt: new Date().toISOString(),
    source: result ? 'minecraft-legacy-ping' : 'minecraft-unreachable',
    server: HOST
  });
};
