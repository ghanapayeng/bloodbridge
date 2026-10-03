# 🧩 02 - Backend Modules & REST API Surface

This document provides a comprehensive architectural breakdown of the 14 backend packages in `src/main/java/BloodBridge`, their internal services, and their corresponding REST endpoints.

---

## Package Overview

```
src/main/java/BloodBridge/
├── analytics/     -> 7-day demand forecasting & shortage risk index
├── audit/         -> Enterprise audit logs & admin operational telemetry
├── auth/          -> User accounts, BCrypt security, sessions, OTP verification
├── call/          -> WebRTC peer-to-peer audio/video signaling relay
├── chat/          -> Request-scoped direct messaging & unread counters
├── common/        -> Global exception handler, BloodGroup enum, Compatibility, Haversine
├── config/        -> SecurityConfig, DotenvLoader, DevDataInitializer, WebConfig
├── donation/      -> Completed donation logs & digital verification certificates
├── donor/         -> Donor profiles, 90-day cooldown rules, Smart Matching engine
├── escalation/    -> Emergency broadcast escalations for critical unfulfilled requests
├── export/        -> CSV report generation for requests, donations, and inventory
├── inventory/     -> Hospital blood bank ledger, reservations, expiry alerts
├── notification/  -> Real-time in-app notification center & badge counts
├── request/       -> Emergency blood requests, volunteer interests, timeline, fraud detection
└── transfer/      -> Inter-hospital blood bag transfers & cold-chain status
```

---

## Detailed Module Analysis

### 1. `BloodBridge.auth` (Authentication & Identity)
- **Key Classes**:
  - [`UserAccount`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/UserAccount.java): User entity storing normalized email, BCrypt password hash, full name, phone number, and user role (`ROLE_USER`, `ROLE_HOSPITAL`, `ROLE_ADMIN`).
  - [`AuthService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/AuthService.java): Manages registration, credential verification, and user resolution from Spring Security context.
  - [`OtpService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/OtpService.java) & [`OtpVerification`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/OtpVerification.java): Generates 6-digit numeric OTPs with 5-minute expiry windows for phone/email validation.
- **REST Endpoints**:
  - `POST /api/auth/register` (Public) - Create a new user account.
  - `POST /api/auth/login` (Public) - Validate credentials, create HTTP session.
  - `GET /api/auth/csrf` (Public) - Return CSRF token metadata for client headers.
  - `GET /api/auth/me` (Authenticated) - Return current user profile without password data.
  - `POST /api/auth/logout` (Authenticated) - Invalidate session and clear security context.
  - `POST /api/auth/otp/send` (Public) - Dispatch a one-time verification code.
  - `POST /api/auth/otp/verify` (Public) - Validate submitted OTP.

---

### 2. `BloodBridge.donor` (Donor Management & Smart Matching)
- **Key Classes**:
  - [`DonorProfile`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorProfile.java): One-to-one record with `UserAccount`. Stores blood group, city, availability toggle, GPS coordinates (`latitude`, `longitude`), and `lastDonationDate`.
  - [`DonorEligibilityService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorEligibilityService.java): Enforces safety checks:
    - 90-day cooldown interval since last donation.
    - Age restriction (18 - 65 years).
    - Weight threshold (minimum 50 kg).
  - [`SmartDonorMatchingService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/SmartDonorMatchingService.java):
    - Computes distance via Haversine formula from [`LocationUtils`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/common/LocationUtils.java).
    - Evaluates compatibility via [`BloodCompatibility`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/common/BloodCompatibility.java).
    - Produces a composite score (0-100) weighting compatibility, physical proximity, and eligibility status.
- **REST Endpoints**:
  - `GET /api/donor-profiles/me` - Get current donor profile.
  - `PUT /api/donor-profiles/me` - Create or update donor profile.
  - `GET /api/donor-profiles/me/eligibility` - Check donation eligibility and days remaining until cooldown expires.
  - `GET /api/donors` - Basic donor filter by blood group and city.
  - `GET /api/donors/smart-match` - Ranked multi-factor candidate list.
  - `GET /api/donors/verify/{token}` (Public) - Public certificate verification portal.

---

### 3. `BloodBridge.request` (Emergency Blood Requests & Fraud Detection)
- **Key Classes**:
  - [`BloodRequest`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequest.java): Emergency request with hospital name, city, urgency (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), units requested/fulfilled, status (`OPEN`, `FULFILLED`, `CANCELLED`), risk level (`LOW`, `MEDIUM`, `HIGH`), and coordinates.
  - [`BloodRequestInterest`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequestInterest.java): Donor response to volunteer for a request. Has status `PENDING`, `ACCEPTED`, `DECLINED`.
  - [`RequestFraudDetectionService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestFraudDetectionService.java): Evaluates suspicious patterns (e.g. excessive units requested > 10, multiple open requests from same user within 24 hours, suspicious keywords) and flags high-risk requests for admin review.
  - [`RequestTimelineEvent`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestTimelineEvent.java): Audit trail tracking lifecycle stages (`CREATED`, `DONOR_RESPONDED`, `ACCEPTED`, `FULFILLED`).
- **REST Endpoints**:
  - `POST /api/blood-requests` - Create emergency request (runs fraud detector).
  - `GET /api/blood-requests` - Query open requests with filters.
  - `GET /api/blood-requests/emergencies` - Feed of high/critical urgency requests.
  - `GET /api/blood-requests/me` - Requests created by logged-in user.
  - `POST /api/blood-requests/{id}/interests` - Register donor willingness to help.
  - `PATCH /api/blood-requests/{id}/interests/{interestId}` - Accept/decline volunteer.
  - `PATCH /api/blood-requests/{id}/fulfillment` - Update fulfilled units count.
  - `PATCH /api/blood-requests/{id}/status` - Update status (`OPEN`, `FULFILLED`, `CANCELLED`).
  - `GET /api/blood-requests/{id}/timeline` - Get chronological event trail.
  - `GET /api/blood-requests/flagged` (Admin) - View fraud-flagged requests.
  - `PATCH /api/blood-requests/{id}/risk-review` (Admin) - Approve or modify risk score.

---

### 4. `BloodBridge.inventory` (Hospital Blood Bank Ledger)
- **Key Classes**:
  - [`BloodInventory`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/inventory/BloodInventory.java): Tracks hospital stock by facility name, blood group, component type (WHOLE_BLOOD, PRBC, PLATELETS, FFP), batch number, collection date, expiry date, units available, reserved units, and status (`AVAILABLE`, `RESERVED`, `EXPIRED`, `DEPLETED`).
  - [`BloodInventoryService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/inventory/BloodInventoryService.java): Automates stock reservations, issues/transfusions, and generates critical shortage and expiry alerts.
- **REST Endpoints**:
  - `GET /api/inventory` - Search inventory by facility, group, status.
  - `POST /api/inventory` - Log new incoming blood batch.
  - `GET /api/inventory/alerts` - List expiring (< 5 days) and critical low-stock units.
  - `PATCH /api/inventory/{id}/reserve` - Place units on reserve for surgery.
  - `PATCH /api/inventory/{id}/issue` - Deduct units for patient transfusion.

---

### 5. `BloodBridge.transfer` (Inter-Hospital Blood Bag Transfers)
- **Key Classes**:
  - [`BloodTransfer`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/transfer/BloodTransfer.java): Manages inter-facility transfers between source and destination hospitals. Tracks blood group, units, cold-chain temperature notes, and status (`REQUESTED`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`).
- **REST Endpoints**:
  - `POST /api/transfers` - Request transfer between hospital facilities.
  - `GET /api/transfers` - List transfers filtered by hospital or status.
  - `PATCH /api/transfers/{id}/status` - Advance cold-chain delivery state.

---

### 6. `BloodBridge.chat` & `BloodBridge.call` (Emergency Communication & WebRTC)
- **Key Classes**:
  - [`ChatMessage`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/chat/ChatMessage.java): Direct message tied to a specific `BloodRequest`. Supports read receipts and user isolation.
  - [`CallSignal`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/call/CallSignal.java): REST signaling channel for WebRTC. Stores session descriptions (SDP offer/answer) and ICE candidate payloads between caller and receiver.
- **REST Endpoints**:
  - `GET /api/chat/requests/{requestId}/conversations/{otherUserId}` - Fetch conversation.
  - `POST /api/chat/requests/{requestId}/conversations/{otherUserId}` - Send message.
  - `GET /api/chat/unread-count` - Total unread message badge count.
  - `POST /api/call/signal` - Relay SDP or ICE signaling packet.
  - `GET /api/call/poll` - Long-poll/retrieve pending incoming signals.

---

### 7. `BloodBridge.analytics`, `BloodBridge.escalation`, `BloodBridge.export`, `BloodBridge.audit`
- **Analytics**:
  - [`DemandPredictionService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/analytics/DemandPredictionService.java): Projects 7-day blood demand based on historical request volume, seasonal trends, and current inventory.
  - `GET /api/analytics/demand-forecast`
- **Escalation**:
  - [`EscalationService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/escalation/EscalationService.java): Broadcasts critical alerts to all compatible donors in the region when high-urgency requests remain unfulfilled after a set time.
  - `POST /api/escalation/escalate` & `GET /api/escalation/escalation-logs`
- **Export**:
  - [`ExportService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/export/ExportService.java): Generates compliant CSV downloads.
  - `GET /api/export/requests/csv`, `GET /api/export/donations/csv`, `GET /api/export/inventory/csv`
- **Audit & Admin**:
  - [`AuditLogService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/audit/AuditLogService.java): Records security actions, admin reviews, and inventory mutations.
  - `GET /api/admin/telemetry`, `GET /api/admin/audit-logs`
