# BloodBridge - Lifesaving Blood Donation & Emergency Coordination Platform

BloodBridge is a full-stack blood donation and emergency request matching platform connecting voluntary blood donors directly with patients and healthcare facilities in urgent need.

---

## 🌟 Key Features

1. **Authentication & Session Management**
   - Secure BCrypt password hashing.
   - Session-based authentication with automatic CSRF protection (`CookieCsrfTokenRepository`).
   - One-click Demo Account login on the login page for rapid evaluation.

2. **Donor Profile & Real-Time Availability**
   - Manage donor blood group, city, contact phone, and availability status.
   - 1-click availability toggle on the dashboard with live status indicators.
   - Real-time profile completion progress calculation.

3. **Smart Donor Search & Matching**
   - Multi-factor donor ranking algorithm based on:
     - Exact blood group compatibility rules (e.g., O- can donate to all, AB+ can receive from all).
     - Geographic proximity (matching city).
     - Availability status.
     - Donation cooldown eligibility (90-day cooldown check).
   - "Request Help" flow prefilling emergency requests for chosen donors.

4. **Blood Request Lifecycle & Interest Volunteering**
   - Create urgent blood requests with hospital, city, urgency level, and notes.
   - Donors can volunteer ("Help") on compatible open requests with automatic medical compatibility validation.
   - Requester dashboard to review responding donors, view contact information, and Accept or Decline volunteers.
   - Request status management (`OPEN`, `FULFILLED`, `CANCELLED`).

5. **Donation History & Impact Metrics**
   - Record completed donations with date, units, and associated request.
   - Automatically synchronizes the donor profile's `lastDonationDate`.
   - Live metrics calculating completed donations, requests helped, and lives impacted (1 donation = up to 3 lives).

---

## 🚀 Getting Started

### Prerequisites
- **Java 25** or later (`java -version`)
- **Maven** (included via `./mvnw` wrapper)

### Running Locally (Default: Zero-Setup Dev Mode)
By default, the application runs with the `dev` profile using an embedded file-backed H2 database in MySQL compatibility mode. It automatically seeds sample data on first start:

```bash
./mvnw spring-boot:run
```

Once started, open your browser and navigate to:
**[http://localhost:8080](http://localhost:8080)**

#### Default Demo Credentials:
- **Email:** `demo@bloodbridge.org`
- **Password:** `BloodBridge123!`
- *(Or click the **"Demo Account (One-click fill)"** button on the Login page)*

---

### Running in Production (MySQL Mode)
To run against a live MySQL database with Flyway migrations:

1. Create a MySQL database and user:
   ```sql
   CREATE DATABASE bloodbridge;
   CREATE USER 'bloodbridge_app'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON bloodbridge.* TO 'bloodbridge_app'@'localhost';
   FLUSH PRIVILEGES;
   ```
2. Start the application with the `prod` profile:
   ```bash
   SPRING_PROFILES_ACTIVE=prod DB_PASSWORD=your_password ./mvnw spring-boot:run
   ```

---

## 🧪 Testing

Run the automated test suite including unit tests and full integration tests:

```bash
./mvnw test
```

### Test Coverage Highlights:
- **`BloodBridgeIntegrationTests`**: End-to-end integration tests validating:
  - User registration & login.
  - Profile retrieval & availability updates.
  - Blood request creation & open requests listing.
  - Donor interest registration & requester acceptance.
  - Donation logging & donor `lastDonationDate` auto-synchronization.
  - Multi-criteria donor search with blood group compatibility.
- **`BloodGroupParsingTests`**: Validation of flexible blood group notation parsing (`A+`, `B-`, `O+`, `A_POSITIVE`, etc.) across JSON payloads and URL query parameters.

---

## 📂 Project Structure

```
bloodbridge/
├── src/main/java/BloodBridge/
│   ├── auth/              # Authentication, user entities, DTOs, and controllers
│   ├── common/            # BloodGroup enum, compatibility rules, exception handlers
│   ├── config/            # Spring Security, Web MVC converters, dev data seeder
│   ├── donation/          # Donation recording, history, and statistics
│   ├── donor/             # Donor profiles, search & matching algorithms
│   └── request/           # Blood requests, donor interest volunteering lifecycle
├── src/main/resources/
│   ├── static/            # Frontend HTML, CSS, and modern API-driven JS modules
│   │   ├── api.js         # Central API client, CSRF injection, toast notifications
│   │   ├── dashboard.html # Donor dashboard with live stats and request management
│   │   ├── find-donors.html # Smart donor search with compatibility scores
│   │   ├── history.html   # Donation and volunteer activity history + logging modal
│   │   ├── request.html   # Emergency blood request submission form
│   │   ├── donor.html     # Donor profile management
│   │   ├── login.html     # Authentication with one-click demo login
│   │   └── signup.html    # User registration
│   ├── db/migration/      # Flyway SQL migrations for production MySQL
│   ├── application.properties
│   └── application-dev.properties
└── src/test/java/BloodBridge/ # Automated unit and integration tests
```
