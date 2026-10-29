# 🚀 BloodBridge Vercel Services & Container Deployment Guide

This guide details how BloodBridge is configured to deploy **all-on-Vercel** without rewriting the Java Spring Boot backend into Node.js. It leverages **Vercel Services** and the **Vercel Container Runtime (Fluid Compute)** to run the frontend and backend side-by-side in the same project.

---

## 🏗️ Architecture Overview

```
                                      [ User Browser ]
                                             │
                             HTTPS Request   ▼  (e.g., bloodbridge.vercel.app)
                     ┌──────────────────────────────────────────────┐
                     │                 Vercel Edge                  │
                     │  (Root Project Router & Reverse Proxy)       │
                     └──────────────┬───────────────────┬───────────┘
                                    │                   │
                     Path: /*       │                   │ Path: /api/*
                     (Static/HTML)  ▼                   ▼ (REST Endpoints)
                     ┌─────────────────────┐     ┌────────────────────────────┐
                     │   Service: "web"    │     │     Service: "backend"     │
                     │                     │     │                            │
                     │ - Static Edge Asset │     │ - Java 25 Spring Boot      │
                     │   Hosting (public/) │     │ - Container runtime (OCI)  │
                     │ - Clean URLs        │     │ - Dynamic $PORT listener   │
                     │ - Zero server lag   │     │ - Dockerfile.vercel        │
                     └─────────────────────┘     └──────────────┬─────────────┘
                                                                │
                                              ┌─────────────────┴─────────────────┐
                                              │                                   │
                                              ▼                                   ▼
                                   [ TiDB Cloud / MySQL ]                  [ Resend API ]
                                   (Database via TLS)                      (Email OTP Delivery)
```

### Key Architectural Highlights:
1. **True Polyglot Monorepo on Vercel**: Uses `vercel.json`'s `services` block to deploy the static web frontend (`web`) and the Spring Boot backend (`backend`) within the same Vercel project and URL.
2. **Containerized Spring Boot Function**: The backend is packaged via `Dockerfile.vercel` using a multi-stage Eclipse Temurin 25 build, running as an unprivileged user (`bloodbridge`) and dynamically binding to the runtime port specified by `$PORT`.
3. **No Node.js Backend Rewrite**: 100% of existing Java Spring Boot controllers, entities, repositories, Spring Security rules, CSRF verification, and database migrations are preserved intact.
4. **No Production `localhost:8080` Dependency**: In production, requests to `/api/*` are routed directly to the containerized `backend` service. Any legacy proxy gateway explicitly rejects unconfigured calls with HTTP 502 rather than attempting invalid localhost connections.
5. **Secure Credential Isolation**: Database credentials (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) and the Resend API key (`RESEND_API_KEY`, `RESEND_FROM`) are stored in Vercel's encrypted Environment Variables and injected server-side only.

---

## 📁 Deployment Configuration Files

| File | Purpose |
| :--- | :--- |
| `vercel.json` | Configures Vercel Services declaring `web` (static frontend) and `backend` (containerized Spring Boot), with rewrites mapping `/api/(.*)` to the backend service. |
| `Dockerfile.vercel` | Multi-stage production container build (Eclipse Temurin JDK 25 builder $\rightarrow$ JRE 25 runtime). Runs unprivileged, optimizes JVM RAM allocation (`-XX:MaxRAMPercentage=75.0`), and binds to `${PORT:-8080}`. |
| `src/main/resources/application.properties` | Configures `server.port=${PORT:8080}` ensuring dynamic port listening on Vercel while preserving local 8080 default. |
| `build.js` | Synchronizes static assets from `src/main/resources/static/` into `public/` for the `web` service. |
| `api/[...path].js` & `api/health.js` | Serverless gateway fallback. Removes production dependency on `localhost:8080` and provides diagnostic status. |
| `.vercelignore` | Excludes local build directories (`target/`, `node_modules/`, `data/`) while preserving Java sources, Maven wrappers, and Dockerfiles needed for Vercel container builds. |
| `.env.example` | Template documenting all environment variables for Vercel Project Settings. |

---

## ⚙️ Step-by-Step Vercel Deployment

### Step 1: Push Repository to GitHub
Ensure all changes are committed and pushed to your remote repository:
```bash
git add .
git commit -m "Configure Vercel Services and Container deployment for Spring Boot backend"
git push origin main
```

### Step 2: Configure Environment Variables in Vercel
Before triggering the deployment build, set your secrets in the Vercel Dashboard under:
**Project Settings $\rightarrow$ Environment Variables**:

| Variable Name | Environment | Example / Value | Description |
| :--- | :--- | :--- | :--- |
| `DB_URL` | Production, Preview | `jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/blood_bridge?useSSL=true&sslMode=VERIFY_IDENTITY` | TiDB / MySQL JDBC connection string |
| `DB_USERNAME` | Production, Preview | `your_user.root` | Database username |
| `DB_PASSWORD` | Production, Preview | `your_secure_password` | Database password |
| `RESEND_API_KEY` | Production, Preview | `re_123456789...` | API key from [Resend](https://resend.com/api-keys) |
| `RESEND_FROM` | Production, Preview | `onboarding@resend.dev` (or verified domain) | Verified sender email |
| `SPRING_PROFILES_ACTIVE` | Production, Preview | `local` | Spring active profile |
| `JPA_SHOW_SQL` | Production, Preview | `false` | SQL query logging |

> [!IMPORTANT]
> Mark `DB_PASSWORD` and `RESEND_API_KEY` as **Secret** / encrypted in Vercel to protect them from exposure.

---

### Step 3: Deploy via Vercel Web Dashboard (Recommended)

1. Navigate to [vercel.com/new](https://vercel.com/new).
2. Select your `bloodbridge` Git repository and click **Import**.
3. Under **Project Settings**:
   - Vercel will read `vercel.json` and automatically recognize the multi-service configuration (`web` and `backend`).
4. Click **Deploy**.
5. Vercel will:
   - Build the `web` service by running `node build.js` and publishing `public/`.
   - Build the `backend` service by executing `Dockerfile.vercel` and deploying the container image to Fluid compute.
   - Bind public routes: `/api/*` to the container and all other paths to the static web frontend.

---

### Step 4: Deploy via Vercel CLI (Alternative)

If using the command line:

```bash
# 1. Login to Vercel
vercel login

# 2. Link your local project
vercel link

# 3. Pull or set environment variables
vercel env add DB_URL
vercel env add DB_USERNAME
vercel env add DB_PASSWORD
vercel env add RESEND_API_KEY

# 4. Deploy to production
vercel --prod --non-interactive
```

---

## 🔍 Verification & Health Checks

Once deployed, verify that both the frontend and containerized backend are operational:

1. **Frontend Check**:
   Open `https://<your-project>.vercel.app/` in your browser. Verify the landing page, clean URLs (`/login`, `/signup`, `/dashboard`), and asset loading.

2. **Backend CSRF Endpoint**:
   ```bash
   curl -i https://<your-project>.vercel.app/api/auth/csrf
   ```
   Should return HTTP 200 with the CSRF token and `XSRF-TOKEN` cookie from the Spring Boot container.

3. **Backend Health Diagnostic**:
   ```bash
   curl -s https://<your-project>.vercel.app/api/health
   ```

---

## ⚠️ Requirements & Limitations (Requirement 12)

If your deployment encounters issues or container builds fail, verify the following account-level requirements:

1. **Vercel Services Beta Access**:
   - Vercel Services is in **Beta**. It is available across Hobby, Pro, and Enterprise tiers.
   - If your account does not yet have Services beta enabled or the project was created under legacy project settings, the build logs will report `services is not supported in vercel.json`.
   - **Resolution**: Enable Beta Features in your Vercel Account / Team Settings under **Feature Previews $\rightarrow$ Services**, or deploy the project via Vercel Container Function directly (`vercel project update --framework container`).

2. **Vercel Container Runtime Limits**:
   - Maximum compressed layer size: 2 GB.
   - Maximum total image size: 15 GB.
   - Fluid compute memory: 1024 MB default (sufficient for Eclipse Temurin JRE 25 with `-XX:MaxRAMPercentage=75.0`).
   - The Spring Boot container must bind to `$PORT` (configured automatically in `Dockerfile.vercel` and `application.properties`).

3. **Outbound Database Connectivity**:
   - Your MySQL / TiDB database host must be accessible from Vercel's serverless IP pool (or allow `0.0.0.0/0` with TLS encryption enabled, standard for TiDB Cloud Serverless clusters).
