# 🌐 07 - Vercel Deployment Architecture & Edge Proxy

> **Notice**: This document details the architectural configuration implemented to enable one-click, zero-configuration deployment of BloodBridge on Vercel.

---

## 1. Challenge & Architectural Problem

BloodBridge was originally architected as a Spring Boot application where the JVM serves both:
1. REST API endpoints (`/api/...`)
2. Static assets directly from `src/main/resources/static/` on port 8080.

When deploying to **Vercel**:
- Vercel is a serverless & frontend hosting platform (Node.js, Edge runtimes, Static assets).
- Vercel cannot run a persistent Spring Boot / JVM application or Docker containers.
- If imported directly as a Java repository, Vercel fails with `"No framework detected"` or `"No build output found"`.
- Furthermore, if static assets are hosted on Vercel while the backend runs on a separate container domain, browsers enforce strict cross-origin cookie rules (blocking `JSESSIONID` and `XSRF-TOKEN`), and Spring Security without CORS rejects cross-origin preflight `OPTIONS` requests with HTTP 403 Forbidden.

---

## 2. Solution: The Edge Hybrid Architecture

We implemented a zero-friction hybrid architecture:

```
[ Client Browser ] 
       │ 
       ▼ (Same-Origin HTTPS)
[ Vercel Edge Global CDN ]
  ├── Static Pages (11 HTML, 8 CSS, 13 JS) served from /public
  └── Serverless Function (/api/[...path].js)
            │
            ▼ (Secure Server-to-Server Relay)
      [ Spring Boot Backend ] (Upstream API Service)
            │
            ▼ (Distributed MySQL Protocol)
      [ TiDB Cloud Database ]
```

### Key Components

1. **`package.json` & `build.js`**:
   - Gives Vercel native Node.js project recognition.
   - `build.js` synchronizes all web assets from `src/main/resources/static/` to `public/` during `node build.js`.

2. **`vercel.json`**:
   - Specifies `buildCommand: "node build.js"` and `outputDirectory: "public"`.
   - Enables `cleanUrls: true` for extensionless navigation (`/dashboard`, `/login`, etc.).
   - Rewrites `/api/:path*` to `/api/[...path]`.
   - Configures maximum execution duration (60s) for cold-start tolerance.
   - Adds HTTP security headers and immutable asset caching.

3. **Edge Serverless Function Proxy (`api/[...path].js`)**:
   - Catches all `/api/*` endpoints.
   - Forwards method, path, query parameters, headers, and body to `BACKEND_URL` (default: `http://localhost:8080`).
   - Automatically handles preflight `OPTIONS` requests.
   - **Cookie Sanitization**: Strips upstream `Domain` attributes and applies `Path=/; SameSite=Lax`, ensuring the browser stores `JSESSIONID` and `XSRF-TOKEN` on the Vercel domain.
   - **Cold-Start Protection**: Returns structured JSON with HTTP 503 if the backend is spinning up from idle, allowing client retry without connection drop.

4. **Health Check Endpoint (`api/health.js`)**:
   - `GET /api/health` probes the upstream backend and returns gateway latency and health diagnostics.

5. **Spring Boot CORS Configuration (`SecurityConfig.java`)**:
   - Added `CorsConfigurationSource` allowing `https://*.vercel.app` and `http://localhost:*` with `allowCredentials(true)`.

6. **Client Layer Enhancements (`api.js`, `login.js`, `signup.js`, `certificate.js`)**:
   - Added `buildApiUrl` and `getApiBaseUrl` with fallback logic.
   - Set `credentials: "include"` for session preservation.
   - Added non-blocking toast detection for initial backend spin-up delays.

---

## 3. Verification & Validation

The solution includes an automated 7-step test suite in `test-proxy.js`:
- `Test 1`: Asset build into `public/`
- `Test 2`: Serverless handlers integrity
- `Test 3`: `/api/health` response
- `Test 4`: CSRF token proxying and cookie rewriting
- `Test 5`: CORS preflight `OPTIONS` resolution
- `Test 6`: Static file serving & clean URLs
- `Test 7`: Full end-to-end registration -> login -> authenticated session verification

Run with:
```bash
npm test
```
All 7 tests passed successfully.
