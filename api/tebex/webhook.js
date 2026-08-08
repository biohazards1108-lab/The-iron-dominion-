const crypto = require('crypto');

const ALLOWED_METHODS = new Set(['POST']);

function readRawBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    const limit = 1024 * 1024;

    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > limit) {
        reject(Object.assign(new Error('Request body too large'), { statusCode: 413 }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

function safeEqualHex(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string') return false;
  const left = Buffer.from(a, 'utf8');
  const right = Buffer.from(b, 'utf8');
  return left.length === right.length && crypto.timingSafeEqual(left, right);
}

function tebexSignature(rawBody, secret) {
  // Tebex signs SHA-256(body) using the webhook secret as the HMAC key.
  const bodyHash = crypto.createHash('sha256').update(rawBody).digest('hex');
  return crypto.createHmac('sha256', secret).update(bodyHash, 'utf8').digest('hex');
}

function json(res, status, payload) {
  res.statusCode = status;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.end(JSON.stringify(payload));
}

module.exports = async function handler(req, res) {
  if (!ALLOWED_METHODS.has(req.method)) {
    res.setHeader('Allow', 'POST');
    return json(res, 405, { error: 'Method not allowed' });
  }

  const secret = process.env.TEBEX_WEBHOOK_SECRET;
  if (!secret) {
    console.error('TEBEX_WEBHOOK_SECRET is not configured');
    return json(res, 503, { error: 'Webhook is not configured' });
  }

  let rawBody;
  try {
    rawBody = await readRawBody(req);
  } catch (error) {
    return json(res, error.statusCode || 400, { error: 'Unable to read request body' });
  }

  const suppliedSignature = req.headers['x-signature'];
  const expectedSignature = tebexSignature(rawBody, secret);
  if (!safeEqualHex(suppliedSignature, expectedSignature)) {
    return json(res, 401, { error: 'Invalid webhook signature' });
  }

  let payload;
  try {
    payload = JSON.parse(rawBody.toString('utf8'));
  } catch {
    return json(res, 400, { error: 'Invalid JSON' });
  }

  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    return json(res, 400, { error: 'Invalid webhook payload' });
  }

  if (payload.type === 'validation.webhook') {
    if (typeof payload.id !== 'string' || payload.id.length < 1) {
      return json(res, 400, { error: 'Validation webhook is missing id' });
    }
    return json(res, 200, { id: payload.id });
  }

  if (typeof payload.id !== 'string' || typeof payload.type !== 'string') {
    return json(res, 400, { error: 'Webhook is missing id or type' });
  }

  // Payment fulfillment is intentionally not performed here yet. The event is
  // authenticated above, but durable idempotency/storage and the Minecraft
  // fulfillment bridge must be configured before any purchase can grant items.
  // Returning 200 tells Tebex the authenticated event was accepted by the API.
  if (payload.type === 'payment.completed') {
    console.log(JSON.stringify({
      event: 'tebex.payment.completed.accepted',
      webhookId: payload.id,
      transactionId: payload.subject && payload.subject.transaction_id || null,
      productCount: Array.isArray(payload.subject && payload.subject.products)
        ? payload.subject.products.length
        : 0,
    }));
  } else {
    console.log(JSON.stringify({ event: 'tebex.webhook.accepted', webhookId: payload.id, type: payload.type }));
  }

  return json(res, 200, { received: true, id: payload.id });
};

module.exports.config = {
  api: {
    bodyParser: false,
  },
};
