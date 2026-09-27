# BloodBridge Backend Architecture & API Specification

The BloodBridge backend is built with **Spring Boot 4.1.1** (Java 25) and provides RESTful JSON APIs under `/api`. Web pages are served statically from `src/main/resources/static`.

API calls require an authenticated session, except for public endpoints (registration, login, OTP endpoints, CSRF token issuance, and public donor certificate verification). Passwords are never stored in plaintext and are hashed using **BCrypt** (`BCryptPasswordEncoder`).

State-changing endpoints (`POST`, `PUT`, `PATCH`, `DELETE`) require a CSRF token: first obtain a token via `GET /api/auth/csrf` (or read the `XSRF-TOKEN` cookie), and include it in the `X-XSRF-TOKEN` HTTP header for all mutations.

---

## 🗄️ Database Architecture & JPA Entities

The application uses **Spring Data JPA** with **Hibernate ORM**. The database schema is automatically updated via `spring.jpa.hibernate.ddl-auto=update`.

### Entity & Table Reference

| Table Name | Entity Class | Purpose |
| --- | --- | --- |
| `app_users` | `UserAccount` | User accounts, normalized emails, BCrypt password hashes, roles, and timestamps. |
| `donor_profiles` | `DonorProfile` | One-to-one donor profiles: blood group, contact phone, city, availability status, latitude, longitude, and last donation date. |
| `blood_requests` | `BloodRequest` | Emergency requests: requester, blood group, hospital, city, urgency, units needed, fulfilled count, status (`OPEN`, `FULFILLED`, `CANCELLED`), risk level, and notes. |
| `blood_request_interests` | `BloodRequestInterest` | Donor responses to open requests. Includes status (`PENDING`, `ACCEPTED`, `DECLINED`) and unique constraint per user/request. |
| `donations` | `Donation` | Completed donation records with date, units given, hospital, and optional request link. |
| `blood_inventories` | `BloodInventory` | Hospital blood bank stock: facility name, blood group, units available, reserved units, component type, batch number, collection date, expiry date, and status. |
| `blood_transfers` | `BloodTransfer` | Inter-hospital blood bag transfers: source/destination facilities, blood group, units, transfer status (`REQUESTED`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`), and temperature notes. |
| `chat_messages` | `ChatMessage` | Request-linked in-app direct messaging between donor and requester, with read receipts and timestamps. |
| `call_signals` | `CallSignal` | WebRTC audio/video peer-to-peer signaling packets (offer, answer, ICE candidates, hangup). |
| `app_notifications` | `AppNotification` | Real-time user notifications: type, message, read state, target URL, and creation timestamp. |
| `otp_verifications` | `OtpVerification` | One-Time Password tokens for email/phone verification with expiry tracking. |
| `request_timeline_events` | `RequestTimelineEvent` | Immutable event audit trail tracking each stage of a blood request's lifecycle. |
| `request_escalation_logs` | `RequestEscalationLog` | Records of emergency broadcast escalations triggered for unfulfilled critical requests. |
| `audit_logs` | `AuditLog` | Enterprise security and audit logs tracking administrative actions, inventory edits, and transfers. |

---

## 🔌 Complete REST API Surface

### 1. Authentication & Session Management (`/api/auth`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Register a new user account. |
| `POST` | `/api/auth/login` | Public | Authenticate user and initiate an HTTP session. |
| `GET` | `/api/auth/csrf` | Public | Retrieve CSRF token metadata (`token`, `headerName`, `parameterName`). |
| `GET` | `/api/auth/me` | Authenticated | Get current authenticated user details. |
| `POST` | `/api/auth/logout` | Authenticated | Terminate the active HTTP session. |
| `POST` | `/api/auth/otp/send` | Public | Request an OTP code for phone/email verification. |
| `POST` | `/api/auth/otp/verify` | Public | Validate a submitted OTP code. |

### 2. Donor Profiles & Smart Matching (`/api/donor-profiles`, `/api/donors`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/donor-profiles/me` | Authenticated | Retrieve the authenticated user's donor profile. |
| `PUT` | `/api/donor-profiles/me` | Authenticated | Create or update the authenticated user's donor profile. |
| `GET` | `/api/donor-profiles/me/eligibility` | Authenticated | Evaluate 90-day cooldown and medical donation eligibility. |
| `GET` | `/api/donors` | Authenticated | Basic donor search by blood group and city. |
| `GET` | `/api/donors/smart-match` | Authenticated | Multi-factor smart ranking with Haversine distance and compatibility scores. |
| `GET` | `/api/donors/verify/{token}` | Public | Verify public donor credentials and certificate badge authenticity. |

### 3. Blood Requests & Volunteering (`/api/blood-requests`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/blood-requests` | Authenticated | Submit an emergency blood request with automated fraud detection. |
| `GET` | `/api/blood-requests` | Authenticated | List open blood requests with optional filters (`bloodGroup`, `city`, `urgency`). |
| `GET` | `/api/blood-requests/emergencies` | Authenticated | Broadcast feed of high-priority and critical emergency requests. |
| `GET` | `/api/blood-requests/me` | Authenticated | List requests submitted by the logged-in user. |
| `GET` | `/api/blood-requests/interests/me` | Authenticated | List requests where the logged-in donor has volunteered help. |
| `POST` | `/api/blood-requests/{id}/interests` | Authenticated | Register donor interest to volunteer on a compatible request. |
| `GET` | `/api/blood-requests/{id}/interests` | Request Owner | View list of responding donors and contact details. |
| `PATCH` | `/api/blood-requests/{id}/interests/{interestId}` | Request Owner | Accept or decline a donor's interest response. |
| `PATCH` | `/api/blood-requests/{id}/fulfillment` | Request Owner | Increment or set fulfilled unit counts for a request. |
| `PATCH` | `/api/blood-requests/{id}/status` | Request Owner | Transition request status (`OPEN`, `FULFILLED`, `CANCELLED`). |
| `GET` | `/api/blood-requests/{id}/timeline` | Authenticated | View immutable lifecycle timeline events for a request. |
| `GET` | `/api/blood-requests/flagged` | Admin | List requests flagged by fraud detection. |
| `PATCH` | `/api/blood-requests/{id}/risk-review` | Admin | Review and update risk score or dismiss flag. |

### 4. Donations & Certificates (`/api/donations`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/donations` | Authenticated | Log a completed donation (auto-synchronizes `lastDonationDate`). |
| `GET` | `/api/donations/me` | Authenticated | Retrieve user's donation history and impact summary statistics. |
| `GET` | `/api/donations/certificates` | Authenticated | Fetch earned digital donation certificates and verification tokens. |

### 5. Blood Bank Inventory (`/api/inventory`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/inventory` | Authenticated | Query stock by facility name, status, or blood group. |
| `GET` | `/api/inventory/alerts` | Authenticated | Retrieve critical shortage, low stock, and expiry warnings. |
| `POST` | `/api/inventory` | Authenticated | Ingest a new blood batch into the inventory ledger. |
| `PATCH` | `/api/inventory/{id}/reserve` | Authenticated | Reserve blood units for a surgery or patient. |
| `PATCH` | `/api/inventory/{id}/issue` | Authenticated | Issue/transfuse blood units from inventory. |

### 6. Inter-Hospital Blood Transfers (`/api/transfers`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/transfers` | Authenticated | Initiate an emergency transfer between medical centers. |
| `GET` | `/api/transfers` | Authenticated | List transfers filtered by source, destination, or status. |
| `PATCH` | `/api/transfers/{id}/status` | Authenticated | Advance transfer state (`REQUESTED`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`). |

### 7. Emergency Communication (`/api/chat`, `/api/call`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/chat/requests/{requestId}/conversations/{otherUserId}` | Authenticated | Retrieve message history for a specific blood request. |
| `POST` | `/api/chat/requests/{requestId}/conversations/{otherUserId}` | Authenticated | Send an in-app message. |
| `GET` | `/api/chat/unread-count` | Authenticated | Get total count of unread messages across all active requests. |
| `POST` | `/api/call/signal` | Authenticated | Relay WebRTC signaling payload (offer, answer, candidate, hangup). |
| `GET` | `/api/call/poll` | Authenticated | Poll for pending WebRTC call signals targeted to the user. |

### 8. Notifications (`/api/notifications`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/notifications` | Authenticated | Retrieve the authenticated user's notification list. |
| `GET` | `/api/notifications/unread-count` | Authenticated | Get count of unread notifications for navbar badge. |
| `PATCH` | `/api/notifications/{id}/read` | Authenticated | Mark a single notification as read. |
| `PATCH` | `/api/notifications/read-all` | Authenticated | Mark all notifications as read. |

### 9. Analytics, Escalation & Exports (`/api/analytics`, `/api/escalation`, `/api/export`, `/api/admin`)

| Method | Endpoint | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/analytics/demand-forecast` | Authenticated | 7-day predicted blood demand and shortage risk index. |
| `POST` | `/api/escalation/escalate` | Authenticated | Trigger emergency broadcast for unfulfilled critical requests. |
| `GET` | `/api/escalation/escalation-logs` | Authenticated | View emergency escalation broadcast history. |
| `GET` | `/api/export/requests/csv` | Authenticated | Download blood request records in CSV format. |
| `GET` | `/api/export/donations/csv` | Authenticated | Download donation history in CSV format. |
| `GET` | `/api/export/inventory/csv` | Authenticated | Download blood inventory stock in CSV format. |
| `GET` | `/api/admin/telemetry` | Admin | Platform-wide operational KPIs and telemetry metrics. |
| `GET` | `/api/admin/audit-logs` | Admin | Comprehensive system security and activity audit logs. |

---

## 🔒 Security & Privacy Architecture

- **Session Authentication**: Uses Spring Security's `HttpSessionSecurityContextRepository` and `DaoAuthenticationProvider`.
- **Password Protection**: Passwords are saved as salted BCrypt hashes. Passwords and hashes are omitted from all DTO responses.
- **CSRF Token Handling**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` sets an `XSRF-TOKEN` cookie readable by JavaScript. The client (`api.js`) automatically attaches `X-XSRF-TOKEN` to all mutating requests.
- **Contact Privacy**: Contact phone numbers and exact donor coordinates are withheld during search discovery. They are only disclosed when a volunteer explicitly offers assistance and the requester accepts the offer.
- **Environment Configuration**: Secrets and database credentials are kept out of version control using `.env` (loaded automatically by `DotenvLoader`).
