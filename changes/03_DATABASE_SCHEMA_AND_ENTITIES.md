# 🗄️ 03 - Database Schema & JPA Entities

This document defines the relational database schema, JPA entities, foreign key relationships, indices, and enumeration mappings used in BloodBridge.

---

## Entity Relationship Overview

```
                      ┌──────────────────┐
                      │    app_users     │
                      │  (UserAccount)   │
                      └────────┬─────────┘
                               │
       ┌───────────────────────┼────────────────────────┬──────────────────────┐
       │ 1:1                   │ 1:N                    │ 1:N                  │ 1:N
       ▼                       ▼                        ▼                      ▼
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌──────────────┐
│donor_profiles│     │  blood_requests  │     │    donations     │     │notifications │
└──────────────┘     └─────────┬────────┘     └──────────────────┘     └──────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            │ 1:N                                 │ 1:N
            ▼                                     ▼
 ┌──────────────────────┐              ┌──────────────────────┐
 │blood_request_interests              │request_timeline_events
 └──────────────────────┘              └──────────────────────┘
```

---

## Table & Entity Specifications

### 1. `app_users` ([`UserAccount.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/UserAccount.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Columns**:
  - `email` (VARCHAR(150), Unique, Not Null) - Stored in lowercase trimmed format.
  - `password_hash` (VARCHAR(255), Not Null) - BCrypt encrypted password.
  - `full_name` (VARCHAR(100), Not Null) - User's display name.
  - `phone_number` (VARCHAR(20), Nullable) - Contact telephone.
  - `role` (VARCHAR(30), Not Null) - E.g. `ROLE_USER`, `ROLE_HOSPITAL`, `ROLE_ADMIN`.
  - `verified` (BOOLEAN, Default False) - Identity verification flag.
  - `created_at` (TIMESTAMP) & `updated_at` (TIMESTAMP)

### 2. `donor_profiles` ([`DonorProfile.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donor/DonorProfile.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Key**: `user_id` -> `app_users.id` (Unique, 1-to-1 relationship)
- **Columns**:
  - `blood_group` (VARCHAR(15), Not Null) - Standardized `BloodGroup` enum.
  - `city` (VARCHAR(80), Not Null)
  - `latitude` (DOUBLE, Nullable) & `longitude` (DOUBLE, Nullable) - Coordinates for Haversine proximity calculations.
  - `is_available` (BOOLEAN, Default True) - Active volunteer availability toggle.
  - `last_donation_date` (DATE, Nullable) - Benchmark for the 90-day cooldown period.
  - `age` (INT, Nullable) & `weight_kg` (DOUBLE, Nullable) - Medical safety factors.
  - `verification_token` (VARCHAR(64), Unique, Nullable) - Public certificate token.

### 3. `blood_requests` ([`BloodRequest.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequest.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Key**: `requester_id` -> `app_users.id`
- **Columns**:
  - `patient_name` (VARCHAR(100), Nullable)
  - `hospital_name` (VARCHAR(120), Not Null)
  - `city` (VARCHAR(80), Not Null)
  - `latitude` (DOUBLE, Nullable) & `longitude` (DOUBLE, Nullable)
  - `blood_group` (VARCHAR(15), Not Null)
  - `units_needed` (INT, Not Null)
  - `units_fulfilled` (INT, Default 0)
  - `urgency` (VARCHAR(20), Not Null) - `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
  - `status` (VARCHAR(20), Not Null) - `OPEN`, `FULFILLED`, `CANCELLED`.
  - `risk_level` (VARCHAR(20), Default `LOW`) - `LOW`, `MEDIUM`, `HIGH` (calculated by `RequestFraudDetectionService`).
  - `notes` (TEXT, Nullable)
  - `created_at` (TIMESTAMP)

### 4. `blood_request_interests` ([`BloodRequestInterest.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/BloodRequestInterest.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Keys**:
  - `request_id` -> `blood_requests.id`
  - `donor_user_id` -> `app_users.id`
- **Constraint**: Unique on `(request_id, donor_user_id)` - Prevents duplicate offers.
- **Columns**:
  - `status` (VARCHAR(20), Default `PENDING`) - `PENDING`, `ACCEPTED`, `DECLINED`.
  - `message` (VARCHAR(255), Nullable)
  - `created_at` (TIMESTAMP)

### 5. `donations` ([`Donation.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/donation/Donation.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Key**: `donor_id` -> `app_users.id`, `request_id` -> `blood_requests.id` (Nullable)
- **Columns**:
  - `donation_date` (DATE, Not Null)
  - `units_donated` (INT, Default 1)
  - `hospital_name` (VARCHAR(120), Not Null)
  - `certificate_code` (VARCHAR(64), Unique, Nullable)

### 6. `blood_inventories` ([`BloodInventory.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/inventory/BloodInventory.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Columns**:
  - `facility_name` (VARCHAR(120), Not Null)
  - `blood_group` (VARCHAR(15), Not Null)
  - `component_type` (VARCHAR(30), Not Null) - `WHOLE_BLOOD`, `PRBC`, `PLATELETS`, `FFP`.
  - `batch_number` (VARCHAR(50), Unique, Not Null)
  - `units_available` (INT, Not Null)
  - `units_reserved` (INT, Default 0)
  - `collection_date` (DATE, Not Null)
  - `expiry_date` (DATE, Not Null)
  - `status` (VARCHAR(20), Not Null) - `AVAILABLE`, `RESERVED`, `EXPIRED`, `DEPLETED`.

### 7. `blood_transfers` ([`BloodTransfer.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/transfer/BloodTransfer.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Columns**:
  - `source_facility` (VARCHAR(120), Not Null)
  - `destination_facility` (VARCHAR(120), Not Null)
  - `blood_group` (VARCHAR(15), Not Null)
  - `units` (INT, Not Null)
  - `status` (VARCHAR(20), Not Null) - `REQUESTED`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`.
  - `temperature_notes` (VARCHAR(200), Nullable) - Cold chain assurance logs.
  - `initiated_at` (TIMESTAMP) & `delivered_at` (TIMESTAMP, Nullable)

### 8. `chat_messages` ([`ChatMessage.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/chat/ChatMessage.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Keys**:
  - `request_id` -> `blood_requests.id`
  - `sender_id` -> `app_users.id`
  - `recipient_id` -> `app_users.id`
- **Columns**:
  - `message_text` (TEXT, Not Null)
  - `is_read` (BOOLEAN, Default False)
  - `sent_at` (TIMESTAMP)

### 9. `call_signals` ([`CallSignal.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/call/CallSignal.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Columns**:
  - `caller_user_id` (BIGINT, Not Null)
  - `receiver_user_id` (BIGINT, Not Null)
  - `signal_type` (VARCHAR(30), Not Null) - `OFFER`, `ANSWER`, `CANDIDATE`, `HANGUP`.
  - `payload_json` (TEXT, Not Null) - SDP session or ICE candidate data.
  - `is_consumed` (BOOLEAN, Default False)
  - `created_at` (TIMESTAMP)

### 10. `app_notifications` ([`AppNotification.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/notification/AppNotification.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Key**: `recipient_id` -> `app_users.id`
- **Columns**:
  - `notification_type` (VARCHAR(40), Not Null)
  - `title` (VARCHAR(100), Not Null)
  - `body` (VARCHAR(255), Not Null)
  - `target_url` (VARCHAR(255), Nullable)
  - `is_read` (BOOLEAN, Default False)
  - `created_at` (TIMESTAMP)

### 11. `otp_verifications` ([`OtpVerification.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/auth/OtpVerification.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Columns**:
  - `identifier` (VARCHAR(150), Not Null) - Email or phone number.
  - `otp_code` (VARCHAR(10), Not Null)
  - `expires_at` (TIMESTAMP, Not Null)
  - `is_used` (BOOLEAN, Default False)

### 12. `request_timeline_events` ([`RequestTimelineEvent.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/request/RequestTimelineEvent.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Key**: `request_id` -> `blood_requests.id`
- **Columns**:
  - `stage` (VARCHAR(30), Not Null) - `CREATED`, `DONOR_RESPONDED`, `ACCEPTED`, `FULFILLED`, `CANCELLED`.
  - `description` (VARCHAR(255), Not Null)
  - `actor_name` (VARCHAR(100), Nullable)
  - `event_timestamp` (TIMESTAMP)

### 13. `request_escalation_logs` ([`RequestEscalationLog.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/escalation/RequestEscalationLog.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Foreign Key**: `request_id` -> `blood_requests.id`
- **Columns**:
  - `escalation_level` (VARCHAR(30), Not Null)
  - `donors_notified_count` (INT, Default 0)
  - `escalation_timestamp` (TIMESTAMP)

### 14. `audit_logs` ([`AuditLog.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/main/java/BloodBridge/audit/AuditLog.java))
- **Primary Key**: `id` (BIGINT, Auto-Increment)
- **Columns**:
  - `actor_user_id` (BIGINT, Nullable)
  - `action_type` (VARCHAR(50), Not Null)
  - `resource_type` (VARCHAR(50), Not Null)
  - `resource_id` (VARCHAR(50), Nullable)
  - `details` (TEXT, Nullable)
  - `ip_address` (VARCHAR(45), Nullable)
  - `timestamp` (TIMESTAMP)
