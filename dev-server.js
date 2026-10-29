/**
 * BloodBridge Local Development Server (Simulates Vercel)
 * Runs locally on port 3000, serving static files and proxying /api to backend.
 * Zero npm dependencies required.
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');
const BACKEND_BASE = (process.env.BACKEND_URL || 'http://localhost:8080').replace(/\/$/, '');

// Ensure public directory is built
if (!fs.existsSync(PUBLIC_DIR) || fs.readdirSync(PUBLIC_DIR).length === 0) {
  console.log('🔄 Building static assets first...');
  require('./build.js');
}

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2'
};

const proxyHandler = require('./api/[...path].js');
const healthHandler = require('./api/health.js');

const server = http.createServer(async (req, res) => {
  // 1. API proxying
  if (req.url === '/api/health' || req.url.startsWith('/api/health?')) {
    return healthHandler(req, res);
  }
  if (req.url.startsWith('/api/') || req.url === '/api') {
    return proxyHandler(req, res);
  }

  // 2. Static file serving
  const urlPath = req.url.split('?')[0];
  let filePath = path.join(PUBLIC_DIR, urlPath === '/' ? 'index.html' : urlPath);

  // If file doesn't exist and has no extension, check for .html (Clean URLs)
  if (!fs.existsSync(filePath) && !path.extname(filePath)) {
    const htmlCandidate = filePath + '.html';
    if (fs.existsSync(htmlCandidate)) {
      filePath = htmlCandidate;
    }
  }

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': contentType });
    return fs.createReadStream(filePath).pipe(res);
  }

  // Fallback to index.html or 404
  res.writeHead(404, { 'Content-Type': 'text/html; charset=UTF-8' });
  res.end('<h1>404 Not Found</h1><p>The requested page was not found on BloodBridge.</p><a href="/">Return Home</a>');
});

server.listen(PORT, () => {
  console.log(`\n==================================================`);
  console.log(`🩸 BloodBridge Vercel Dev Server Running!`);
  console.log(`🌐 Local URL:       http://localhost:${PORT}`);
  console.log(`🔗 Backend Proxy:   ${BACKEND_BASE}`);
  console.log(`📂 Static Root:     ${PUBLIC_DIR}`);
  console.log(`==================================================\n`);
});
