# 🚀 BloodBridge Vercel Deployment Guide

This guide explains how BloodBridge is configured to deploy and run seamlessly on **Vercel** with full static frontend hosting and a built-in Edge Serverless API Gateway.

---

## 🏗️ Architecture Overview

```
                                  [ Browser User ]
                                         │
                         HTTPS requests  ▼  (e.g., bloodbridge.vercel.app)
                  ┌──────────────────────────────────────────────┐
                  │                 Vercel Edge                  │
                  │                                              │
                  │  Static Web Pages:       Serverless Proxy:   │
                  │  (public/*.html, css, js) (api/[...path].js)  │
                  └──────────────┬───────────────────┬───────────┘
                                 │                   │
                                 │                   │ /api/* proxied with
                                 │                   │ cookie & CSRF rewriting
                                 │                   ▼
                                 │      [ Spring Boot Backend ]
                                 │      (Upstream API Service)
                                 │                   │
                                 │                   ▼
                                 │      [ TiDB Cloud Database ]
                                 │      (Distributed Cloud MySQL)
```

### Why this architecture?
1. **Zero Configuration on Vercel**: Vercel natively serves the static web assets (`public/`) at edge speed worldwide.
2. **Built-in Serverless Proxy (`api/[...path].js`)**: Relays all `/api/*` REST endpoints to the live Spring Boot backend.
3. **Cookie & CSRF Rewrite**: Transparently re-writes `Set-Cookie` (`JSESSIONID`, `XSRF-TOKEN`) to bind directly to the client's `*.vercel.app` domain with `SameSite=Lax`, completely eliminating third-party cookie restrictions.
4. **CORS Preflight Acceleration**: Handles `OPTIONS` preflight requests directly on Vercel's edge with HTTP 200.
5. **Cold-Start Resilience**: Detects backend server cold starts and provides graceful JSON feedback rather than connection drops.

---

## ⚡ Option 1: Deploy via Vercel Web UI (Recommended)

1. **Push your repository** to GitHub.
2. Go to [https://vercel.com/new](https://vercel.com/new) and log in.
3. **Import** the `bloodbridge` repository.
4. In the **Configure Project** screen:
   - **Framework Preset**: Leave as `Other` (or auto-detected).
   - **Root Directory**: `./` (Default root).
   - **Build Command**: `node build.js` (Configured automatically via `vercel.json` / `package.json`).
   - **Output Directory**: `public` (Configured automatically via `vercel.json`).
5. *(Optional)* **Environment Variables**:
   - `BACKEND_URL`: URL of your deployed Spring Boot backend (defaults to `http://localhost:8080`). Set this to your live production backend URL.
6. Click **Deploy**!

Within **30-45 seconds**, your app will be live at `https://<your-project>.vercel.app`!

---

## 💻 Option 2: Deploy via Vercel CLI

If you have the Vercel CLI installed:

```bash
# 1. Install Vercel CLI if needed
npm install -g vercel

# 2. Login to Vercel
vercel login

# 3. Deploy to preview
vercel

# 4. Deploy to production
vercel --prod
```

---

## 🧪 Local Testing (Simulate Vercel Locally)

You can run the application locally exactly as it behaves on Vercel:

```bash
# 1. Build assets into /public
npm run build

# 2. Start local Vercel-simulation dev server (port 3000)
npm run dev
```

Visit **`http://localhost:3000`** in your browser. All `/api/*` requests will be proxied to the live backend.

### Run Automated Verification Test Suite
```bash
npm test
```
This runs 7 automated end-to-end checks:
- ✅ Asset build verification
- ✅ Serverless handlers integrity
- ✅ `/api/health` connectivity check
- ✅ CSRF token extraction and cookie rewriting
- ✅ CORS preflight OPTIONS resolution
- ✅ Static file serving and clean URL routing
- ✅ End-to-end registration, login, and session validation

---

## 🔍 Health Check & Diagnostics

Once deployed to Vercel, you can test connectivity at any time by visiting:

```
https://<your-app>.vercel.app/api/health
```

Example response:
```json
{
  "status": "HEALTHY",
  "gateway": "BloodBridge Vercel Edge Serverless",
  "backendTarget": "http://localhost:8080",
  "backendStatus": "ONLINE",
  "latency": "1450ms",
  "error": null,
  "timestamp": "2026-10-30T00:00:00.000Z"
}
```

---

## 📁 Key Files Added for Vercel

| File | Purpose |
| :--- | :--- |
| `vercel.json` | Root Vercel configuration specifying build command, public directory, security headers, clean URLs, and rewrites. |
| `package.json` | Node.js project manifest enabling Vercel build detection and scripts (`build`, `dev`, `test`). |
| `build.js` | Zero-dependency build script that syncs assets from `src/main/resources/static` to `public/`. |
| `api/[...path].js` | Catch-all Vercel Serverless Function proxying all API requests, rewriting cookies, and handling CSRF. |
| `api/index.js` | Handles root `/api` endpoint requests. |
| `api/health.js` | Dedicated health-check route returning gateway and backend diagnostics. |
| `.vercelignore` | Excludes Java source files, Maven artifacts, and binaries to keep deployment uploads fast and small. |
| `dev-server.js` | Local HTTP development server simulating Vercel behavior on port 3000. |
| `test-proxy.js` | Automated 7-step test suite validating all Vercel functionality. |
