# 🏗️ 00 - System Architecture & Engineering Design

This document details the high-level architecture, design patterns, security model, and execution lifecycle of the **BloodBridge** platform.

---

## 1. Technical Stack

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Runtime & Language** | Java 25 (OpenJDK / Eclipse Temurin) | Utilizes modern Java features, records, pattern matching, switch expressions. |
| **Framework** | Spring Boot 4.1.1 | Starters: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`. |
| **Persistence / ORM** | Hibernate 6+ / Spring Data JPA | Auto DDL management via `spring.jpa.hibernate.ddl-auto=update`. |
| **Databases** | TiDB Cloud & H2 / MySQL / PostgreSQL | Production runs on **TiDB Cloud** (distributed MySQL-wire compatible database). Local/Test runs H2 in-memory or MySQL; configured via `.env` / environment variables. |
| **Hosting & PaaS** | Render Cloud Platform | Live production instance: [https://bloodbridge-wzdp.onrender.com](https://bloodbridge-wzdp.onrender.com) |
| **Authentication & Security** | Spring Security 6+ | Stateful HTTP Session (`HttpSessionSecurityContextRepository`), salted BCrypt password hashing, Cookie-backed CSRF protection. |
| **Frontend Runtime** | Native Web Standards | HTML5, CSS3 Custom Properties Design System, ES6+ Vanilla JavaScript. Zero external UI framework runtime dependencies. |
| **Signaling & Comms** | WebRTC & REST Signaling | In-memory peer-to-peer signaling store for audio/video calls (`/api/call`) + request-scoped chat threads (`/api/chat`). |
| **Containerization** | Docker Multi-Stage Build | Maven 3.9 builder stage with Temurin 25 JDK -> minimal Temurin 25 JRE runtime container. |

---

## 2. Layered Architecture

```
                    ┌────────────────────────────────────────────────────────┐
                    │                      WEB BROWSER                       │
                    │   HTML5 / CSS3 / Vanilla JS (api.js, dashboard.js...)  │
                    └───────────────────────────┬────────────────────────────┘
                                                │ HTTPS / REST (JSON)
                                                │ Headers: X-XSRF-TOKEN
                                                ▼
                    ┌────────────────────────────────────────────────────────┐
                    │               SPRING SECURITY FILTER CHAIN             │
                    │  CsrfFilter (CookieCsrfTokenRepository)                │
                    │  SecurityContextPersistence / Session Filter           │
                    │  Role-based authorization checks (/api/**)             │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
                    ┌────────────────────────────────────────────────────────┐
                    │             SPRING BOOT CONTROLLER LAYER               │
                    │  AuthController, BloodRequestController,               │
                    │  DonorProfileController, BloodInventoryController...   │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
                    ┌────────────────────────────────────────────────────────┐
                    │                 SERVICE BUSINESS LOGIC                 │
                    │  SmartDonorMatchingService, DonorEligibilityService    │
                    │  RequestFraudDetectionService, DemandPredictionService │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
                    ┌────────────────────────────────────────────────────────┐
                    │             SPRING DATA JPA REPOSITORIES               │
                    │  UserAccountRepository, BloodRequestRepository...      │
                    └───────────────────────────┬────────────────────────────┘
                                                │
                                                ▼
                    ┌────────────────────────────────────────────────────────┐
                    │              DATABASE (H2 / MySQL / Postgres)          │
                    └────────────────────────────────────────────────────────┘
```

---

## 3. Security & CSRF Token Flow

Security is configured in [`SecurityConfig.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/config/SecurityConfig.java):

1. **Stateful Session Authentication**:
   - `UserAccount` acts as the security principal.
   - Authentication is performed via `POST /api/auth/login` using `DaoAuthenticationProvider` with `BCryptPasswordEncoder`.
   - On successful login, Spring Security stores the authentication in the user's `HttpSession`.
   - Passwords and BCrypt hashes are strictly excluded from DTO outputs.

2. **CSRF Protection Architecture**:
   - Spring Security uses `CookieCsrfTokenRepository.withHttpOnlyFalse()` which emits a cookie named `XSRF-TOKEN`.
   - State-changing HTTP methods (`POST`, `PUT`, `PATCH`, `DELETE`) require the token to be sent in the `X-XSRF-TOKEN` HTTP request header.
   - The shared frontend client [`api.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/api.js) automatically:
     - Fetches `/api/auth/csrf` on application boot or prior to mutating requests.
     - Automatically attaches the header `X-XSRF-TOKEN: <token>` to all mutating calls.
     - Caches the CSRF token in memory (`_cachedCsrf`).

3. **Public vs Authenticated Endpoints**:
   - **Public**:
     - Static assets: `/`, `/*.html`, `/*.css`, `/*.js`, `/images/**`.
     - Auth: `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/csrf`, `POST /api/auth/otp/**`.
     - Public verification: `GET /api/donors/verify/{token}`.
   - **Admin Only**:
     - `GET /api/admin/telemetry`, `GET /api/admin/audit-logs`, `GET /api/blood-requests/flagged`, `PATCH /api/blood-requests/{id}/risk-review`.
   - **Authenticated (Any logged-in user)**:
     - All other `/api/**` routes.

---

## 4. Configuration & Environment Management

1. **Custom Dotenv Loader ([`DotenvLoader.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/config/DotenvLoader.java))**:
   - Runs before Spring Boot initializes.
   - Checks for a `.env` file in the working directory and loads key-value pairs into Java `System.setProperty()`, enabling seamless local overrides without polluting system-wide variables.
   - Fallback defaults are configured in [`application.properties`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/application.properties) and [`application-dev.properties`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/application-dev.properties).

2. **Database Profiles & Cloud Deployment**:
   - **Production (Render + TiDB)**: Hosted on **Render** connecting to **TiDB Cloud** via MySQL dialect:
     - URL format: `jdbc:mysql://${DB_HOST}:${DB_PORT:4000}/${DB_NAME}?useSSL=true&sslMode=VERIFY_IDENTITY`
     - Uses standard MySQL connector `com.mysql.cj.jdbc.Driver` and `org.hibernate.dialect.MySQLDialect`.
     - In Render dashboard, environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and `SPRING_PROFILES_ACTIVE=local` are configured.
   - **Local Development**: Live MySQL (`jdbc:mysql://localhost:3306/blood_bridge`) or custom `.env`.
   - **Testing / In-Memory**: H2 in-memory (`jdbc:h2:mem:bloodbridgedb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`).

---

## 5. Development Data Seeding ([`DevDataInitializer.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/config/DevDataInitializer.java))

To allow immediate local development and testing without manual data entry, the backend automatically seeds:
- Default Admin Account: `admin@bloodbridge.com` (password: `admin123`).
- Default Hospital User: `hospital@citygeneral.org` (password: `password123`).
- Default Donor Accounts: `donor.rahul@example.com`, `donor.priya@example.com`, etc.
- Multiple active `BloodRequest` records (Emergency A+, B-, O-, AB+ requests in critical/high urgency).
- Real inventory batches in `BloodInventory` for City General Hospital and Metro Red Cross.
- Realistic cold-chain `BloodTransfer` records with transit logs.
