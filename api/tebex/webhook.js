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
  // Tebex: SHA-256 the raw JSON body, then HMAC-SHA256 that hash with the webhook secret.
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

  // Never log the secret, signature value, or request body. These diagnostics only
  // tell us whether Tebex reached the function and supplied the expected header.
  const suppliedSignature = req.headers['x-signature'];
  console.log(JSON.stringify({
    event: 'tebex.webhook.request',
    method: req.method,
    hasSignature: typeof suppliedSignature === 'string' && suppliedSignature.length > 0,
    contentType: req.headers['content-type'] || null,
  }));

  const secret = process.env.TEBEX_WEBHOOK_SECRET;
  if (!secret) {
    console.error('TEBEX_WEBHOOK_SECRET is not configured in this deployment');
    return json(res, 503, { error: 'Webhook is not configured' });
  }

  let rawBody;
  try {
    rawBody = await readRawBody(req);
  } catch (error) {
    return json(res, error.statusCode || 400, { error: 'Unable to read request body' });
  }

  const expectedSignature = tebexSignature(rawBody, secret);
  if (!safeEqualHex(suppliedSignature, expectedSignature)) {
    console.warn('Rejected Tebex webhook: invalid signature');
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

  // Tebex endpoint validation: return the validation webhook's ID exactly as JSON.
  if (payload.type === 'validation.webhook') {
    if (typeof payload.id !== 'string' || payload.id.length < 1) {
      return json(res, 400, { error: 'Validation webhook is missing id' });
    }
    console.log(JSON.stringify({ event: 'tebex.validation.accepted', webhookId: payload.id }));
    return json(res, 200, { id: payload.id });
  }

  if (typeof payload.id !== 'string' || typeof payload.type !== 'string') {
    return json(res, 400, { error: 'Webhook is missing id or type' });
  }

  // Payment fulfillment is intentionally not performed here yet. The event is
  // authenticated above, but durable idempotency/storage and the Minecraft
  // fulfillment bridge must be configured before any purchase can grant items.
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
