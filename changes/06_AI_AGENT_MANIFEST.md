# 🤖 06 - AI Agent Manifest & Engineering Guide

> **Primary Objective**: This guide is engineered for AI coding assistants (Gemini, Claude, GPT, Copilot, etc.) and human developers extending or maintaining the BloodBridge repository. Follow these principles to maintain architectural integrity.

---

## 1. Fast Reference Map: "Where is feature X?"

| Feature / Domain | Backend Core Files | Frontend Files |
| :--- | :--- | :--- |
| **Authentication & Users** | [`UserAccount.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/UserAccount.java), [`AuthService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/AuthService.java), [`AuthController.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/AuthController.java) | [`login.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/login.html), [`signup.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/signup.html), [`api.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/api.js) |
| **Security & CSRF** | [`SecurityConfig.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/config/SecurityConfig.java) | [`api.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/api.js) (`getCsrfToken`, `apiFetch`) |
| **Donor Profiles & Rules** | [`DonorProfile.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorProfile.java), [`DonorEligibilityService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorEligibilityService.java) | [`donor.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/donor.html), [`donor.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/donor.js) |
| **Smart Donor Matching** | [`SmartDonorMatchingService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/SmartDonorMatchingService.java), [`BloodCompatibility.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/common/BloodCompatibility.java) | [`find-donors.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/find-donors.html), [`find-donors.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/find-donors.js) |
| **Emergency Requests** | [`BloodRequest.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequest.java), [`BloodRequestService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequestService.java) | [`request.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/request.html), [`request.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/request.js) |
| **Fraud Detection** | [`RequestFraudDetectionService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestFraudDetectionService.java) | Flagged request review in admin controllers |
| **Hospital Inventory** | [`BloodInventory.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/inventory/BloodInventory.java), [`BloodInventoryService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/inventory/BloodInventoryService.java) | [`inventory.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/inventory.html), [`inventory.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/inventory.js) |
| **Inter-Hospital Transfers** | [`BloodTransfer.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/transfer/BloodTransfer.java), [`BloodTransferService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/transfer/BloodTransferService.java) | [`transfers.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/transfers.html), [`transfers.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/transfers.js) |
| **In-App Chat** | [`ChatMessage.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/chat/ChatMessage.java), [`ChatService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/chat/ChatService.java) | Chat modal in [`dashboard.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.html) |
| **WebRTC Audio/Video Call** | [`CallSignal.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/call/CallSignal.java), [`CallService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/call/CallService.java) | WebRTC controller in [`dashboard.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.js) |
| **Certificates & Verification** | [`Donation.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donation/Donation.java) | [`certificate.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/certificate.html), [`verify-donor.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/verify-donor.html) |
| **System Seeding** | [`DevDataInitializer.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/config/DevDataInitializer.java) | Automatically loads mock data on startup |
| **Cloud Hosting (Render)** | [`Dockerfile`](file:///home/ghanapayeng/Desktop/bloodbridge/Dockerfile) | Production URL: [https://bloodbridge-wzdp.onrender.com](https://bloodbridge-wzdp.onrender.com) |
| **Production Database (TiDB)** | [`application.properties`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/application.properties), [`.env.example`](file:///home/ghanapayeng/Desktop/bloodbridge/.env.example) | Distributed MySQL-compatible cloud cluster (port 4000/SSL) |

---

## 2. The 10 Golden Rules for AI Coding

1. **CSRF Header Compliance**:
   - In Spring Boot, all `POST`, `PUT`, `PATCH`, and `DELETE` calls require the `X-XSRF-TOKEN` header.
   - On the frontend, **never call raw `fetch()` directly for mutating endpoints**. Always use `apiFetch(url, options)` from [`api.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/api.js).
2. **Password Security**:
   - Never return raw passwords or BCrypt hashes in response DTOs.
   - Hash all passwords with `BCryptPasswordEncoder` before persisting to `UserAccount`.
3. **Donor Privacy Invariant**:
   - Do not leak private donor phone numbers or exact residential coordinates in public donor search results. Contact details must only be revealed after an explicit interest is `ACCEPTED` by the requester.
4. **Blood Group Standardization**:
   - Always map between string representations (e.g. `"O+"`, `"AB-"`) and enum values (`BloodGroup.O_POSITIVE`, `BloodGroup.AB_NEGATIVE`) using [`BloodGroup`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/common/BloodGroup.java) helper methods.
5. **No Heavy Frontend Framework Injections**:
   - Keep the frontend native (Vanilla HTML/CSS/JS). Do not inject heavy npm packages, Node-based bundlers, or React/Vue dependencies unless explicitly requested by the user.
6. **Safety Rules Integrity**:
   - Medical cooldown is non-negotiable (90 days). Never bypass `DonorEligibilityService` logic without explicit user override.
7. **Environment Secrets**:
   - Keep all secrets, passwords, and sensitive API keys in `.env` (or environment variables). Never commit active secrets or modify `.gitignore` to track `.env`.
8. **DevDataInitializer Safe Seeding**:
   - Seeding should remain idempotent (`count() == 0` checks) to prevent duplicate record insertion across server restarts.
9. **Port 8080 Standard**:
   - The Spring Boot backend serves static web assets directly from `src/main/resources/static` on port 8080. If users open HTML files via Live Server (port 5500), [`api.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/api.js) will warn them to use `http://localhost:8080`.
10. **Test Coverage Verification**:
    - Before finishing any multi-file feature or refactoring, verify that existing tests pass by running `./mvnw test`.

---

## 3. Standard Recipe: Adding a New Feature

When adding a new feature (e.g., "Blood Donation Camp Management"):

1. **Entity**: Create `Camp.java` under `BloodBridge/camp/` with `@Entity`, `@Table(name = "donation_camps")`, and audit fields.
2. **Repository**: Create `CampRepository.java` extending `JpaRepository<Camp, Long>`.
3. **DTOs**: Create `CampDtos.java` with request/response records.
4. **Service**: Create `CampService.java` with `@Service` and `@Transactional` methods.
5. **Controller**: Create `CampController.java` with `@RestController`, `@RequestMapping("/api/camps")`.
6. **Security Rules**: If public, add endpoint to `SecurityConfig.java` `authorizeHttpRequests`; otherwise, leave as authenticated.
7. **Frontend**: Create `camp.html`, `camp.css`, and `camp.js` using `apiFetch` from `api.js`.
8. **Navigation**: Add link to navbar in `dashboard.html` and other shared navigation bars.
9. **Tests**: Add unit test class in `src/test/java/BloodBridge/camp/CampServiceTest.java`.
10. **Changelog**: Add an entry summarizing your changes into `changes/`.
