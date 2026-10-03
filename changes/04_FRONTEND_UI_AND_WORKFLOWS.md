# 🖥️ 04 - Frontend UI Architecture & User Workflows

This document details the frontend implementation across all 11 static web pages, styling architecture, and client JavaScript controllers located in `src/main/resources/static`.

---

## 1. Frontend Architectural Principles

1. **Native Standards & Zero Build Tooling**:
   - Built with semantic HTML5, modern CSS3 (Custom Properties & Flex/Grid), and Vanilla JavaScript (ES6+).
   - Requires no npm build, webpack, or Vite step—served directly by Spring Boot from `src/main/resources/static`.
2. **Unified API Gateway Client ([`api.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/api.js))**:
   - **CSRF Token Handling**: Auto-fetches and attaches `X-XSRF-TOKEN` to all mutating (`POST`, `PUT`, `PATCH`, `DELETE`) requests.
   - **Live Server Detection**: Inspects `window.location.port` on DOM load. If opened via VS Code Live Server (port 5500/5501) or `file:`, displays a persistent instructional banner reminding users to visit `http://localhost:8080`.
   - **Toast Feedback System**: Provides animated notifications with `showToast(message, type)`.
   - **Formatters**: Helpers for blood groups (`formatBloodGroup('A_POSITIVE')` -> `'A+'`) and friendly dates (`formatTimeAgo`).

---

## 2. Page Directory & Controller Breakdown

| Page File | Stylesheet | Script Controller | Primary Purpose |
| :--- | :--- | :--- | :--- |
| [`index.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/index.html) | [`style.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/style.css) | [`script.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/script.js) | Public landing portal, live platform metrics ticker, call to action. |
| [`login.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/login.html) | [`login.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/login.css) | [`login.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/login.js) | User authentication, session initialization, error alerts. |
| [`signup.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/signup.html) | [`signup.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/signup.css) | [`signup.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/signup.js) | Registration with multi-step OTP verification and role selector. |
| [`dashboard.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.html) | [`dashboard.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.css) | [`dashboard.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.js) | Primary hub: emergency feed, notifications, WebRTC calls, chat modal. |
| [`donor.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/donor.html) | [`donor.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/donor.css) | [`donor.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/donor.js) | Donor profile management, GPS geocoding, 90-day cooldown countdown. |
| [`find-donors.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/find-donors.html) | [`find-donors.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/find-donors.css) | [`find-donors.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/find-donors.js) | Smart candidate matching, proximity ranking, request creation modal. |
| [`request.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/request.html) | [`request.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/request.css) | [`request.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/request.js) | Emergency blood request creation with immediate validation. |
| [`history.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/history.html) | [`history.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/history.css) | [`history.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/history.js) | User timeline: past donations, requests created, volunteered responses. |
| [`inventory.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/inventory.html) | [`style.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/style.css) | [`inventory.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/inventory.js) | Hospital blood stock manager, expiration alarms, batch logging. |
| [`transfers.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/transfers.html) | [`style.css`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/style.css) | [`transfers.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/transfers.js) | Inter-hospital logistics board, cold chain tracking, state dispatcher. |
| [`certificate.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/certificate.html) | Custom inline/shared | [`certificate.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/certificate.js) | Visual digital donation certificate badge with verification code. |
| [`verify-donor.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/verify-donor.html) | Custom inline/shared | [`verify-donor.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/verify-donor.js) | Public portal to verify authenticity of donor certificates via token. |

---

## 3. End-to-End User Journeys

### Journey A: Emergency Requester Flow
1. **Request Creation** ([`request.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/request.html)):
   - Requester fills in patient name, hospital, city, blood group, units needed, and urgency level.
   - Submits to `POST /api/blood-requests`.
   - The backend runs [`RequestFraudDetectionService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestFraudDetectionService.java) and records a `CREATED` event in [`RequestTimelineEvent`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestTimelineEvent.java).
2. **Donor Matching** ([`find-donors.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/find-donors.html)):
   - System queries `GET /api/donors/smart-match?bloodGroup={bg}&lat={lat}&lng={lng}`.
   - Donors are ranked by compatibility and distance.
3. **Emergency Coordination** ([`dashboard.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.html)):
   - Responding donors register interest (`POST /api/blood-requests/{id}/interests`).
   - Requester reviews donors and accepts an offer.
   - In-app chat is initiated (`/api/chat`) or a WebRTC audio/video call is placed (`/api/call`).

---

### Journey B: Volunteer Donor Flow
1. **Onboarding & Eligibility** ([`signup.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/signup.html) & [`donor.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/donor.html)):
   - User registers, verifies phone with OTP, and sets up blood group and location.
   - [`DonorEligibilityService`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorEligibilityService.java) calculates eligibility and displays days remaining until next eligible donation.
2. **Volunteering & Certificate** ([`dashboard.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.html) & [`certificate.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/certificate.html)):
   - Donor sees emergency broadcast in feed and taps "Volunteer".
   - Once donation completes at hospital, hospital/admin logs donation via `POST /api/donations`.
   - System updates `lastDonationDate`, awards digital certificate with verification token, and generates public verification link.

---

### Journey C: Hospital Blood Bank Staff Flow
1. **Inventory Control** ([`inventory.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/inventory.html)):
   - Staff logs incoming blood bags (`POST /api/inventory`).
   - Receives automatic warnings for units within 5 days of expiry or groups with critical deficits.
2. **Inter-Hospital Transfers** ([`transfers.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/transfers.html)):
   - When facing a shortage, staff dispatches transfer request (`POST /api/transfers`).
   - Driver or medical team updates status from `REQUESTED` -> `DISPATCHED` -> `IN_TRANSIT` -> `DELIVERED`, recording cold-chain storage temperatures.
