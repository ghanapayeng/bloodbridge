/**
 * BloodBridge Vercel Serverless API Proxy
 * Handles all /api/* requests on Vercel and transparently relays them to the backend,
 * ensuring seamless cookie management, CSRF validation, and CORS compliance.
 */

const BACKEND_BASE = (process.env.BACKEND_URL || 'http://localhost:8080').replace(/\/$/, '');

const HOP_BY_HOP_HEADERS = new Set([
  'host',
  'connection',
  'content-length',
  'transfer-encoding',
  'keep-alive',
  'proxy-authenticate',
  'proxy-authorization',
  'te',
  'trailer',
  'upgrade'
]);

async function getRequestBody(req) {
  if (req.body !== undefined && req.body !== null) {
    if (typeof req.body === 'string' || Buffer.isBuffer(req.body)) {
      return req.body;
    }
    return JSON.stringify(req.body);
  }

  if (['GET', 'HEAD'].includes(req.method)) {
    return undefined;
  }

  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', chunk => chunks.push(chunk));
    req.on('end', () => {
      if (chunks.length === 0) return resolve(undefined);
      resolve(Buffer.concat(chunks));
    });
    req.on('error', reject);
  });
}

module.exports = async (req, res) => {
  // Ensure Vercel helper methods exist if running in standard Node.js http server
  if (!res.status) {
    res.status = function (code) {
      res.statusCode = code;
      return res;
    };
  }
  if (!res.json) {
    res.json = function (data) {
      res.setHeader('Content-Type', 'application/json; charset=UTF-8');
      res.end(JSON.stringify(data));
      return res;
    };
  }
  if (!res.send) {
    res.send = function (body) {
      if (typeof body === 'object' && !Buffer.isBuffer(body)) {
        res.setHeader('Content-Type', 'application/json; charset=UTF-8');
        res.end(JSON.stringify(body));
      } else {
        res.end(body);
      }
      return res;
    };
  }

  // 1. Enable standard CORS for all environments
  const origin = req.headers.origin || '*';
  res.setHeader('Access-Control-Allow-Origin', origin);
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-XSRF-TOKEN, X-Requested-With, Accept, Cookie');
  res.setHeader('Access-Control-Expose-Headers', 'Set-Cookie, X-XSRF-TOKEN');
  res.setHeader('Access-Control-Max-Age', '86400');

  // 2. Handle CORS Preflight immediately
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // 3. Health check shortcut
  if (req.url === '/api/health' || req.url === '/api/health/') {
    return res.status(200).json({
      status: 'UP',
      service: 'BloodBridge Vercel Gateway',
      backendTarget: BACKEND_BASE,
      timestamp: new Date().toISOString()
    });
  }

  // 4. Construct upstream target URL
  const upstreamUrl = `${BACKEND_BASE}${req.url}`;

  // 5. Read body payload
  let requestBody;
  try {
    requestBody = await getRequestBody(req);
  } catch (err) {
    console.error('[BloodBridge Gateway] Failed to read request body:', err);
    return res.status(400).json({ error: 'Failed to read request body' });
  }

  // 6. Build forwarded headers
  const forwardHeaders = {};
  for (const [key, val] of Object.entries(req.headers)) {
    if (!HOP_BY_HOP_HEADERS.has(key.toLowerCase()) && val !== undefined) {
      forwardHeaders[key] = val;
    }
  }

  // Set origin and referer to match backend target for upstream same-origin trust
  forwardHeaders['origin'] = BACKEND_BASE;
  forwardHeaders['referer'] = BACKEND_BASE + '/';
  forwardHeaders['x-forwarded-host'] = req.headers.host || 'bloodbridge.vercel.app';
  forwardHeaders['x-forwarded-proto'] = 'https';

  if (requestBody && !forwardHeaders['content-type']) {
    forwardHeaders['content-type'] = 'application/json';
  }

  // 7. Forward request to backend
  try {
    const upstreamResponse = await fetch(upstreamUrl, {
      method: req.method,
      headers: forwardHeaders,
      body: requestBody,
      redirect: 'manual'
    });

    // 8. Forward response status
    res.status(upstreamResponse.status);

    // 9. Forward response headers
    const skipHeaders = new Set(['content-encoding', 'content-length', 'transfer-encoding', 'connection']);
    upstreamResponse.headers.forEach((value, key) => {
      const lower = key.toLowerCase();
      if (!skipHeaders.has(lower) && lower !== 'set-cookie') {
        res.setHeader(key, value);
      }
    });

    // 10. Clean and rewrite Set-Cookie so it binds to the Vercel domain
    let cookies = [];
    if (typeof upstreamResponse.headers.getSetCookie === 'function') {
      cookies = upstreamResponse.headers.getSetCookie();
    } else {
      const sc = upstreamResponse.headers.get('set-cookie');
      if (sc) cookies = [sc];
    }

    if (cookies.length > 0) {
      const rewrittenCookies = cookies.map(cookieStr => {
        return cookieStr
          .replace(/Domain=[^;]+;?\s*/gi, '')
          .replace(/SameSite=[^;]+;?\s*/gi, '')
          + '; SameSite=Lax';
      });
      res.setHeader('Set-Cookie', rewrittenCookies);
    }

    // 11. Send response data
    const dataBuffer = Buffer.from(await upstreamResponse.arrayBuffer());
    return res.send(dataBuffer);

  } catch (error) {
    console.error(`[BloodBridge Gateway Error] Failed to proxy ${req.method} ${req.url} -> ${upstreamUrl}:`, error.message);
    
    return res.status(503).json({
      status: 503,
      error: 'Backend Service Unavailable',
      message: 'BloodBridge backend service is currently starting up or unreachable. Please retry in a few seconds.',
      target: upstreamUrl,
      timestamp: new Date().toISOString()
    });
  }
};
