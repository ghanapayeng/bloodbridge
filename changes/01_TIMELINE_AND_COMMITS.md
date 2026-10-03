# ⏱️ 01 - Timeline & Commit History

This document outlines the complete chronological progression of commits and code alterations in BloodBridge from inception to date.

---

## Commit Log Summary

```
* 77b1897 - (2026-09-27 12:29:25 +0530) Add Docker deployment configuration (ghanapayeng)
* aef2491 - (2026-09-27 09:59:35 +0530) Update complete readme documentation (ghanapayeng)
* 6fc3f83 - (2026-09-27 09:52:16 +0530) first commit (ghanapayeng)
```

---

## Detailed Commit Walkthrough

### 1. Commit `6fc3f83` — "first commit"
- **Timestamp**: `2026-09-27 09:52:16 +0530`
- **Scope**: 141 files changed, 20,655 insertions.
- **Key Deliverables**:
  - **Full Backend Domain Core**:
    - Created Java 25 / Spring Boot 4.1.1 foundational project structure under package `BloodBridge`.
    - Implemented 14 JPA entities: [`UserAccount`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/UserAccount.java), [`DonorProfile`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorProfile.java), [`BloodRequest`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequest.java), [`BloodRequestInterest`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequestInterest.java), [`Donation`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donation/Donation.java), [`BloodInventory`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/inventory/BloodInventory.java), [`BloodTransfer`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/transfer/BloodTransfer.java), [`ChatMessage`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/chat/ChatMessage.java), [`CallSignal`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/call/CallSignal.java), [`AppNotification`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/notification/AppNotification.java), [`OtpVerification`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/OtpVerification.java), [`RequestTimelineEvent`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestTimelineEvent.java), [`RequestEscalationLog`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/escalation/RequestEscalationLog.java), and [`AuditLog`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/audit/AuditLog.java).
    - Established business algorithms:
      - Haversine distance & blood compatibility formula in [`SmartDonorMatchingService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/SmartDonorMatchingService.java).
      - 90-day cooldown and age/weight validation in [`DonorEligibilityService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorEligibilityService.java).
      - Multi-factor fraud scoring in [`RequestFraudDetectionService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestFraudDetectionService.java).
      - 7-day weighted demand projection in [`DemandPredictionService.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/analytics/DemandPredictionService.java).
  - **Full Frontend Suite**:
    - Complete static web portal under `src/main/resources/static`:
      - [`index.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/index.html) (Landing page & metrics)
      - [`login.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/login.html) & [`signup.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/signup.html) (User onboarding & OTP)
      - [`dashboard.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/dashboard.html) (Main hub, real-time alerts, statistics, WebRTC signaling)
      - [`donor.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/donor.html) (Profile management & cooldown tracker)
      - [`find-donors.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/find-donors.html) (Smart matching UI)
      - [`request.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/request.html) (Emergency blood request creator)
      - [`history.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/history.html) (User timeline & historical logs)
      - [`inventory.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/inventory.html) (Hospital blood bank ledger)
      - [`transfers.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/transfers.html) (Inter-hospital logistics tracker)
      - [`certificate.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/certificate.html) & [`verify-donor.html`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/verify-donor.html) (Verifiable digital certificates)
      - [`api.js`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/resources/static/api.js) (Shared HTTP client with CSRF and toast engine)
  - **Automated Tests**:
    - Unit tests for compatibility matrix, blood group parsing, eligibility rules, and fraud detector.
    - Integration tests verifying Spring context and API controller contracts.

---

### 2. Commit `aef2491` — "Update complete readme documentation"
- **Timestamp**: `2026-09-27 09:59:35 +0530`
- **Scope**: 2 files changed, 426 insertions(+), 118 deletions(-).
- **Key Deliverables**:
  - Rewrote [`README.md`](file:///home/ghanapayeng/Desktop/bloodbridge/README.md) into a comprehensive 22KB master document detailing system architecture, user workflows, feature matrix, environment configuration, database tables, and step-by-step setup guides.
  - Rewrote and expanded [`BACKEND.md`](file:///home/ghanapayeng/Desktop/bloodbridge/BACKEND.md) to document all 37+ REST endpoints, HTTP methods, authorization requirements, JPA entities, and security policies.

---

### 3. Commit `77b1897` — "Add Docker deployment configuration"
- **Timestamp**: `2026-09-27 12:29:25 +0530`
- **Scope**: 1 file added, 26 insertions.
- **Key Deliverables**:
  - Added [`Dockerfile`](file:///home/ghanapayeng/Desktop/bloodbridge/Dockerfile) featuring:
    - Stage 1: Build environment using `maven:3.9-eclipse-temurin-25`. Runs `mvn clean package -DskipTests` to package the executable JAR.
    - Stage 2: Lean runtime environment using `eclipse-temurin:25-jre`. Exposes port 8080 and defines `java -jar app.jar` as entrypoint.

---

### 4. Production Hosting & Cloud Database Milestone
- **Hosting Platform**: Render ([https://bloodbridge-wzdp.onrender.com](https://bloodbridge-wzdp.onrender.com))
- **Database Engine**: TiDB Cloud (Serverless / Dedicated distributed MySQL-compatible engine)
- **Deployment Details**:
  - Containerized build triggered via Dockerfile.
  - Port 8080 exposed and routed through Render's reverse proxy with automatic SSL/TLS termination.
  - TiDB connection configured via environment variables with encrypted connection string.

---

### 5. Working Tree Changes (Present Date)
- **`.gitignore` Updates**:
  - Added `.env` and `*.env` to prevent private keys or secrets from leaking into Git.
  - Added `application-local.properties` and `application-prod.properties` for developer-specific local configs.
- **`src/main/resources/static/api.js`**:
  - Streamlined code comments and verified Live Server guidance toast handling.
- **`changes/` Documentation**:
  - Created this dedicated knowledge directory so every AI agent and developer has complete transparency into the codebase history.
