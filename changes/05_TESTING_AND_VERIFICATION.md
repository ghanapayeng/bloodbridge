# 🧪 05 - Testing & Verification Reference

This document catalogs the test suites, verification strategies, and validation scripts created in BloodBridge.

---

## 1. Automated Test Suites

The test files reside in `src/test/java/BloodBridge/`:

```
src/test/java/BloodBridge/
├── analytics/
│   └── DemandPredictionServiceTest.java      -> Tests 7-day projection math & shortage indexes
├── BloodbridgeApplicationTests.java          -> Spring application context boot test
├── BloodBridgeIntegrationTests.java          -> End-to-end MockMvc HTTP integration flows
├── common/
│   ├── BloodCompatibilityTests.java          -> Complete 8x8 blood group matrix verification
│   └── BloodGroupParsingTests.java           -> String/enum conversion & edge case parsing
├── donor/
│   └── DonorEligibilityServiceTest.java      -> 90-day cooldown, age, & weight rule verification
└── request/
    └── RequestFraudDetectionServiceTest.java -> Multi-factor heuristic fraud detection tests
```

---

## 2. Test Suite Breakdown

### A. Blood Compatibility Suite ([`BloodCompatibilityTests.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/test/java/BloodBridge/common/BloodCompatibilityTests.java))
- **Universal Donor Validation**: Asserts that `O_NEGATIVE` is compatible with all recipient blood groups: `O-`, `O+`, `A-`, `A+`, `B-`, `B+`, `AB-`, `AB+`.
- **Universal Recipient Validation**: Asserts that `AB_POSITIVE` can receive blood from all 8 groups.
- **Rh Factor Invariant**: Asserts that Rh-positive blood cannot be infused into Rh-negative recipients.

### B. Eligibility Rule Suite ([`DonorEligibilityServiceTest.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/test/java/BloodBridge/donor/DonorEligibilityServiceTest.java))
- **Cooldown Interval**: Verifies that a donor who gave blood 30 days ago is marked `INELIGIBLE` with 60 days remaining; a donor who gave blood 95 days ago is marked `ELIGIBLE`.
- **Age Bounds**: Rejects candidates under 18 or above 65 years.
- **Weight Threshold**: Rejects candidates weighing under 50 kg.

### C. Fraud Detection Suite ([`RequestFraudDetectionServiceTest.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/test/java/BloodBridge/request/RequestFraudDetectionServiceTest.java))
- **Excessive Units Rule**: Flags any single emergency request requesting > 10 units as `HIGH` risk for administrative verification.
- **High-Frequency Spam**: Flags users submitting 3+ open requests within 24 hours.

### D. Analytics & Forecasting Suite ([`DemandPredictionServiceTest.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/test/java/BloodBridge/analytics/DemandPredictionServiceTest.java))
- Validates the rolling demand algorithm and verifies that deficit calculations correctly trigger a shortage risk score above 70%.

### E. End-to-End Integration Suite ([`BloodBridgeIntegrationTests.java`](file:///home/ghanapayeng/Desktop/bloodbridge/src/test/java/BloodBridge/BloodBridgeIntegrationTests.java))
- Utilizes `MockMvc` with `@SpringBootTest` and `@AutoConfigureMockMvc`.
- Verifies CSRF token acquisition, user registration, authenticated session persistence, and request submission pipelines.

---

## 3. How to Run Tests

### Command Line (Maven Wrapper)
```bash
# Run all automated tests
./mvnw clean test

# Run a specific test class
./mvnw test -Dtest=BloodCompatibilityTests

# Run tests with dev profile
./mvnw test -Dspring.profiles.active=dev
```
