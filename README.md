# BloodBridge - Lifesaving Blood Donation, Emergency Coordination & Inventory Platform

[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Render](https://img.shields.io/badge/Render-Live%20Demo-46E3B7.svg?logo=render&logoColor=white)](https://bloodbridge-wzdp.onrender.com)
[![Database](https://img.shields.io/badge/Database-TiDB%20Cloud-E30C34.svg)](https://tidbcloud.com)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-blue.svg)]()

**BloodBridge** is an enterprise-grade, full-stack emergency blood donation, hospital inventory tracking, and inter-facility coordination platform. It bridges the critical time gap between voluntary blood donors, patients in urgent need, and healthcare networks through real-time matching, intelligent demand forecasting, fraud prevention, and seamless communication.

> 🌐 **Live Production Deployment**:  
> - **Live App**: [https://bloodbridge-wzdp.onrender.com](https://bloodbridge-wzdp.onrender.com) (Hosted on **Render**)  
> - **Production Database**: **TiDB Cloud** (Distributed Cloud MySQL Engine)

> 💡 **AI Assistants & Developer Changelog**:  
> For a complete, structured record of all architectural decisions, code changes, database entities, APIs, frontend pages, and testing specifications created till date, visit the **[changes/](file:///home/ghanapayeng/Desktop/bloodbridge/changes/README.md)** knowledge base directory.

---

## 🌟 Key Features

### 1. 🔍 Smart Donor Search & Proximity Matching
- **Compatibility Matrix**: Validates universal donor/recipient rules (e.g., O- can donate to all red blood cell recipients; AB+ can receive from all).
- **Haversine Geolocation Routing**: Computes spherical distance between patient hospitals and prospective donors in kilometers.
- **Dynamic Multi-Factor Ranking**: Computes real-time match scores combining medical compatibility (40 pts), geographic proximity (30 pts), availability status (15 pts), and days since last donation (15 pts).
- **Urgency Multiplier**: Amplifies prioritization for `CRITICAL` and `HIGH` emergency alerts.

### 2. 👤 Donor Profile & Medical Eligibility Tracking
- **Automated Cooldown Engine**: Enforces mandatory 90-day cooldown periods between whole blood donations.
- **Real-Time Eligibility Countdown**: Displays remaining cooldown days and automated eligibility badges (`ELIGIBLE`, `COOLDOWN`, `INELIGIBLE`).
- **One-Click Availability Toggle**: Allows donors to toggle their real-time standby availability (`Available` / `Busy`) with instant dashboard feedback.
- **Verifiable Donor Badge & QR Code**: Generates secure donor verification tokens with a dedicated public verification portal (`/verify-donor.html`) and QR scanning support.

### 3. 🚨 Emergency Blood Requests & Timeline Lifecycle
- **Multi-Tier Urgency Categorization**: `CRITICAL`, `HIGH`, `MEDIUM`, and `LOW` urgency triage.
- **Volunteering Workflow**: Donors can express interest on compatible open requests with automatic medical compatibility validation.
- **Requester Command Center**: Requesters can inspect responding donors, review contact credentials, and `ACCEPT` or `DECLINE` volunteers.
- **Fulfillment & Timeline Tracking**: Granular tracking of requested vs. fulfilled units, with an immutable timeline audit of events (`OPEN` ➔ `IN_PROGRESS` ➔ `FULFILLED` / `CANCELLED`).

### 4. 🛡️ Fraud Detection & Trust Scoring
- **Automated Risk Screening**: Analyzes request velocity, historical fulfillment anomalies, duplicate submissions, and geographic divergence.
- **Risk Level Flagging**: Classifies submissions into `LOW`, `MEDIUM`, and `HIGH` risk scores.
- **Admin Review Queue**: Flags suspicious requests for administrative review and manual risk override.

### 5. 🏥 Hospital Blood Bank Inventory Management
- **Live Stock Visibility**: Real-time inventory tracking by blood group (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`) across participating medical centers.
- **Batch Intake & Expiration Alerts**: Records collection dates and automatically detects expiring or critical-shortage stock batches.
- **Unit Reservation & Issuance**: Reserve units for impending surgical procedures and track dispatched/transfused units.

### 6. 🚑 Inter-Hospital Emergency Blood Transfers
- **Cross-Facility Logistics**: Dispatch and monitor emergency blood bag transfers between hospitals experiencing localized deficits.
- **Cold-Chain & Stage Tracking**: Full transfer lifecycle states (`REQUESTED` ➔ `DISPATCHED` ➔ `IN_TRANSIT` ➔ `DELIVERED` ➔ `CANCELLED`).
- **Temperature & Dispatch Notes**: Preserves transportation notes and chain-of-custody tracking.

### 7. 📈 Demand Forecasting & Predictive Analytics
- **Regional Demand Predictions**: Evaluates 7-day projected unit requirements by blood group and city based on seasonal trends, current open request velocity, and historical consumption rates.
- **Shortage Vulnerability Indices**: Highlights blood groups at high risk of impending stockouts.

### 8. ⚡ Emergency Escalation Engine
- **Automated Broadcast**: Re-escalates unfulfilled critical requests after configurable time thresholds.
- **Radius Expansion**: Broadens search boundaries to neighboring cities and alerts regional blood banks automatically.

### 9. 💬 In-App Encrypted Chat & WebRTC Calling
- **Request-Linked Real-Time Chat**: Direct, secure messaging between accepted donors and requesters with unread indicators and message histories.
- **WebRTC Emergency Signaling**: Peer-to-peer audio/video call signaling (`/api/call/signal`) allowing urgent audio communication without exposing personal phone numbers.

### 10. 📜 Recognition, Certificates & Data Export
- **Digital Donation Certificates**: Generates verifiable certificates of appreciation complete with donor metadata and QR validation links.
- **CSV Data Export**: One-click operational reports for donations, blood requests, and inventory ledgers.

### 11. 🔐 Security & Session Management
- **Zero-Trust Authentication**: BCrypt password hashing, session-based authentication with `HttpSessionSecurityContextRepository`.
- **CSRF Defense**: Automatic CSRF token issuance (`CookieCsrfTokenRepository` with `X-XSRF-TOKEN`).
- **OTP Verification Flow**: One-Time-Password generation and verification for sensitive workflows and registrations.
- **Fast-Track Demo Access**: One-click demo login button for instant evaluator access.

---

## 🏗️ System Architecture

```
                                  [ Client Browser ]
                     (HTML5 / Modern Vanilla ES6+ / Responsive CSS)
                                          │
                        HTTPS / REST APIs │ CSRF Protected
                                          ▼
                         [ Spring Boot 4.1.1 Application ]
  ┌─────────────────────────────────────────────────────────────────────────────┐
  │                               Security Layer                                │
  │        Spring Security · BCrypt · CSRF Token Repo · Role-Based Auth         │
  └───────┬──────────────┬──────────────┬──────────────┬──────────────┬─────────┘
          │              │              │              │              │
          ▼              ▼              ▼              ▼              ▼
     [Auth & OTP]   [Donor Match]  [Requests &   [Inventory &   [WebRTC & Chat]
      Controller     & Scoring      Escalation]    Transfers]      Controller
          │              │              │              │              │
          ▼              ▼              ▼              ▼              ▼
     [AuthService]  [MatchingSvc]  [RequestSvc]  [InventorySvc] [Chat & CallSvc]
          │              │              │              │              │
          └──────────────┴───────┬──────┴──────────────┴──────────────┘
                                 │ Spring Data JPA
                                 ▼
                    [ Hibernate ORM (MySQL / H2) ]
                                 │
                 ┌───────────────┴───────────────┐
                 ▼                               ▼
       [ Production MySQL ]             [ Development H2 ]
        (Persistent RDBMS)             (Zero-Setup Memory/File)
```

---

## 📂 Project Structure

```
bloodbridge/
├── .env.example                               # Environment variable template
├── pom.xml                                    # Maven project definition (Spring Boot 4.1.1, Java 25)
├── README.md                                  # Platform overview & comprehensive documentation
├── BACKEND.md                                 # Backend REST API specification & database schemas
├── data/                                      # Embedded H2 development database storage
├── src/
│   ├── main/
│   │   ├── java/BloodBridge/
│   │   │   ├── BloodbridgeApplication.java    # Application entry point
│   │   │   ├── analytics/                     # AI/ML demand forecasting & regional predictions
│   │   │   ├── audit/                         # Security audit logs & admin telemetry metrics
│   │   │   ├── auth/                          # Registration, authentication, user entity & OTP verification
│   │   │   ├── call/                          # WebRTC audio/video signaling & polling
│   │   │   ├── chat/                          # In-app peer-to-peer emergency chat messaging
│   │   │   ├── common/                        # BloodGroup enum, compatibility logic, error handler
│   │   │   ├── config/                        # Spring Security, Dotenv loader, dev data seeder
│   │   │   ├── donation/                      # Donation recording, history, and impact statistics
│   │   │   ├── donor/                         # Profiles, smart proximity matching, eligibility engine
│   │   │   ├── escalation/                    # Emergency request escalation & broadcast logging
│   │   │   ├── export/                        # CSV data export endpoints for requests & inventory
│   │   │   ├── inventory/                     # Hospital blood bank stock & reservation management
│   │   │   ├── notification/                  # User notification dispatch & unread count tracking
│   │   │   ├── request/                       # Blood requests, volunteering, timeline & fraud detection
│   │   │   └── transfer/                      # Inter-hospital blood bag transfers & cold-chain
│   │   └── resources/
│   │       ├── application.properties         # Base configuration (MySQL, profiles, Hibernate)
│   │       ├── application-dev.properties     # Dev profile configuration (H2, auto-seeding)
│   │       ├── application-local.properties   # Local override configuration
│   │       └── static/                        # Frontend Single/Multi-Page Web Application
│   │           ├── index.html                 # Modern landing page & live emergency ticker
│   │           ├── login.html / login.js      # Sign-in portal with one-click demo login
│   │           ├── signup.html / signup.js    # Registration wizard with role and OTP verification
│   │           ├── dashboard.html / .js / .css# Operational command center, notifications & chat
│   │           ├── find-donors.html / .js     # Smart donor search with proximity & compatibility
│   │           ├── request.html / .js         # Emergency blood request creation
│   │           ├── donor.html / .js           # Donor profile, medical eligibility & availability
│   │           ├── history.html / .js         # Donation records, impact stats & certificates
│   │           ├── inventory.html / .js       # Blood bank inventory ledger & alerts
│   │           ├── transfers.html / .js       # Inter-hospital transfer dispatch & tracking
│   │           ├── certificate.html / .js     # Digital recognition certificate with QR code
│   │           ├── verify-donor.html / .js    # Public QR verification portal for hospitals
│   │           └── api.js                     # Unified API client with automatic CSRF management
│   └── test/
│       ├── java/BloodBridge/                  # Automated test suite (Integration & Unit tests)
│       └── resources/application.properties   # Test configuration
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 25** or later (`java -version`)
- **Maven** (included via the `./mvnw` wrapper)
- *(Optional)* **MySQL 8.0+** for production deployments

---

### Option A: Zero-Setup Development Mode (Default)
The fastest way to run BloodBridge locally. Uses an embedded, persistent H2 database in MySQL compatibility mode and automatically seeds realistic sample donors, requests, and emergency scenarios on first boot:

```bash
# 1. Clone the repository
git clone https://github.com/your-username/bloodbridge.git
cd bloodbridge

# 2. Run with the dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Once the application starts, navigate to:
**[http://localhost:8080](http://localhost:8080)**

#### 🔑 Pre-Seeded Demo Credentials:
- **Email:** `demo@bloodbridge.org`
- **Password:** `BloodBridge123!`
- *(Or click the **"Demo Account (One-click fill)"** button on the Login page)*

---

### Option B: Local / Production Mode (MySQL)
To connect to a live MySQL instance:

1. **Create the MySQL Database**:
   ```sql
   CREATE DATABASE blood_bridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'bloodbridge_app'@'localhost' IDENTIFIED BY 'your_secure_password';
   GRANT ALL PRIVILEGES ON blood_bridge.* TO 'bloodbridge_app'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Configure Environment Variables**:
   Copy `.env.example` to `.env` and fill in your database credentials:
   ```bash
   cp .env.example .env
   ```
   Edit `.env`:
   ```properties
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=blood_bridge
   DB_USERNAME=bloodbridge_app
   DB_PASSWORD=your_secure_password
   SPRING_PROFILES_ACTIVE=local
   ```
   *(BloodBridge includes an automatic `DotenvLoader` that reads `.env` on application startup without requiring external tools).*

3. **Start the Application**:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🌐 Web Interface Guide

| Page | URL | Purpose |
| --- | --- | --- |
| **Home** | `/index.html` | Hero banner, live emergency requests, platform statistics, and quick navigation. |
| **Login** | `/login.html` | Secure authentication with CSRF handling and one-click demo login. |
| **Sign Up** | `/signup.html` | Multi-step user onboarding with role selection and optional OTP verification. |
| **Dashboard** | `/dashboard.html` | Complete command center: status toggle, active requests, notifications, chat, and call signaling. |
| **Find Donors** | `/find-donors.html` | Smart donor search with distance radius filtering, blood group compatibility, and match scores. |
| **Request Blood** | `/request.html` | Emergency blood request submission form with hospital details, urgency levels, and fraud checks. |
| **Donor Profile** | `/donor.html` | Blood group, city, contact number, 90-day cooldown countdown, and availability toggle. |
| **History** | `/history.html` | Donation activity history, manual donation logging modal, impact counters, and certificate links. |
| **Inventory** | `/inventory.html` | Blood bank stock monitoring by blood group, critical shortage alerts, and unit reservation/issuance. |
| **Transfers** | `/transfers.html` | Inter-hospital blood bag transfers, dispatch status pipeline, and cold-chain notes. |
| **Certificate** | `/certificate.html` | Verifiable digital donation certificate with donor details and QR verification link. |
| **Verify Donor** | `/verify-donor.html` | Public portal to authenticate donor certificates and verification tokens. |

---

## 🔌 API Endpoint Highlights

All backend endpoints are prefixed with `/api`. CSRF protection is active for state-changing HTTP methods (`POST`, `PUT`, `PATCH`, `DELETE`).

### 🔑 Authentication (`/api/auth`)
- `POST /api/auth/register` — Register a new account.
- `POST /api/auth/login` — Sign in and create an authenticated session.
- `GET /api/auth/csrf` — Obtain CSRF token (`X-XSRF-TOKEN`).
- `GET /api/auth/me` — Retrieve the currently authenticated user.
- `POST /api/auth/logout` — Invalidate the session.
- `POST /api/auth/otp/send` & `POST /api/auth/otp/verify` — OTP dispatch and validation.

### 🩸 Donor Profiles & Matching (`/api/donor-profiles`, `/api/donors`)
- `GET /api/donor-profiles/me` — Retrieve current user's donor profile.
- `PUT /api/donor-profiles/me` — Create or update donor profile.
- `GET /api/donor-profiles/me/eligibility` — Check donation cooldown and medical eligibility status.
- `GET /api/donors?bloodGroup=&city=` — Basic donor query.
- `GET /api/donors/smart-match` — Intelligent match score ranking by blood group, geolocation, and availability.
- `GET /api/donors/verify/{token}` — Public verification of a donor badge or certificate.

### 📋 Blood Requests & Volunteering (`/api/blood-requests`)
- `POST /api/blood-requests` — Create an emergency blood request with automated fraud detection.
- `GET /api/blood-requests` — Filter open blood requests by city, blood group, and urgency.
- `GET /api/blood-requests/emergencies` — High-priority broadcast feed of critical emergency requests.
- `GET /api/blood-requests/me` — Requesters view their own submitted requests.
- `POST /api/blood-requests/{id}/interests` — Compatible donors volunteer to assist.
- `GET /api/blood-requests/{id}/interests` — Requester reviews volunteers and contact credentials.
- `PATCH /api/blood-requests/{id}/interests/{interestId}` — Accept or decline volunteer interest.
- `PATCH /api/blood-requests/{id}/fulfillment` — Update fulfilled unit counts.
- `GET /api/blood-requests/{id}/timeline` — Audit log of request lifecycle milestones.

### 🏥 Hospital Inventory & Transfers (`/api/inventory`, `/api/transfers`)
- `GET /api/inventory` — Query hospital stock by facility, status, and blood group.
- `GET /api/inventory/alerts` — Fetch critical shortage and expiring stock warnings.
- `POST /api/inventory` — Ingest new blood stock batch.
- `PATCH /api/inventory/{id}/reserve` & `PATCH /api/inventory/{id}/issue` — Reserve or issue units.
- `POST /api/transfers` — Initiate inter-hospital blood transfer.
- `GET /api/transfers` — List active transfers between facilities.
- `PATCH /api/transfers/{id}/status` — Update transfer lifecycle state (`REQUESTED`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`).

### 💬 Communication & Coordination (`/api/chat`, `/api/call`, `/api/notifications`)
- `GET` & `POST /api/chat/requests/{requestId}/conversations/{otherUserId}` — In-app encrypted messaging.
- `POST /api/call/signal` & `GET /api/call/poll` — WebRTC signaling for emergency voice/video coordination.
- `GET /api/notifications` — Retrieve user alerts and updates.
- `PATCH /api/notifications/{id}/read` — Acknowledge notifications.

### 📊 Analytics & Reporting (`/api/analytics`, `/api/export`, `/api/admin`)
- `GET /api/analytics/demand-forecast` — 7-day predictive blood demand forecast.
- `GET /api/export/requests/csv` — CSV export of blood requests.
- `GET /api/export/donations/csv` — CSV export of donation logs.
- `GET /api/export/inventory/csv` — CSV export of inventory stock.
- `GET /api/admin/telemetry` — System operational telemetry and KPIs.
- `GET /api/admin/audit-logs` — Administrative security audit logs.

*(For full request and response payload schemas, consult [BACKEND.md](file:///home/ghanapayeng/Desktop/bloodbridge/BACKEND.md)).*

---

## 🧪 Testing & Quality Assurance

The codebase includes comprehensive integration and unit tests covering compatibility algorithms, fraud detection, eligibility rules, and full web API lifecycles:

```bash
# Run all automated tests
./mvnw test
```

### Test Suite Highlights:
- **`BloodBridgeIntegrationTests`**: Full end-to-end integration test validating:
  - User registration, session authentication, and CSRF token handling.
  - Profile creation, real-time availability toggling, and eligibility queries.
  - Emergency blood request creation and smart compatibility filtering.
  - Donor interest registration and requester acceptance workflow.
  - Donation logging and automatic `lastDonationDate` synchronization.
- **`BloodCompatibilityTests`**: Exhaustive verification of red blood cell compatibility rules for all 8 blood groups.
- **`BloodGroupParsingTests`**: Ensures robust parsing of flexible blood group notations (`A+`, `B-`, `O_POSITIVE`, etc.) across URL parameters and JSON payloads.
- **`DonorEligibilityServiceTest`**: Validates the 90-day cooldown logic, active deferrals, and countdown days.
- **`RequestFraudDetectionServiceTest`**: Tests rate-limiting heuristic detection for suspicious duplicate requests.
- **`DemandPredictionServiceTest`**: Verifies demand forecast calculations and shortage risk scoring.

---

## 🔒 Security Best Practices

1. **Passwords**: Stored exclusively as salted BCrypt hashes (`BCryptPasswordEncoder`).
2. **CSRF Protection**: Enforced on all mutating operations via Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
3. **Least Privilege**: Sensitive donor contact numbers are hidden during discovery and only revealed to requesters after a volunteer has explicitly offered help and been accepted.
4. **Environment Isolation**: Database credentials and secrets are managed via `.env` files and never committed to version control.
5. **Auditing**: Sensitive transactions (transfers, inventory updates, role modifications) are logged into the `audit_logs` table.

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
