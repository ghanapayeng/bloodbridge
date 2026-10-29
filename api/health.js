/**
 * BloodBridge Vercel Health Check Endpoint
 * GET /api/health
 */
const BACKEND_BASE = (process.env.BACKEND_URL || 'http://localhost:8080').replace(/\/$/, '');

module.exports = async (req, res) => {
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

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');

  const startTime = Date.now();
  let backendStatus = 'UNKNOWN';
  let backendLatency = null;
  let backendError = null;

  try {
    const upstreamRes = await fetch(`${BACKEND_BASE}/api/auth/csrf`, {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    });
    backendLatency = `${Date.now() - startTime}ms`;
    backendStatus = upstreamRes.ok ? 'ONLINE' : `HTTP_${upstreamRes.status}`;
  } catch (err) {
    backendStatus = 'UNREACHABLE';
    backendError = err.message;
  }

  return res.status(backendStatus === 'ONLINE' ? 200 : 503).json({
    status: backendStatus === 'ONLINE' ? 'HEALTHY' : 'DEGRADED',
    gateway: 'BloodBridge Vercel Edge Serverless',
    backendTarget: BACKEND_BASE,
    backendStatus: backendStatus,
    latency: backendLatency,
    error: backendError,
    timestamp: new Date().toISOString()
  });
};
