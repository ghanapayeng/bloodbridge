package BloodBridge.request;

import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodGroup;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestFraudDetectionService {

    private final BloodRequestRepository bloodRequestRepository;

    public RequestFraudDetectionService(BloodRequestRepository bloodRequestRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
    }

    public record RiskAssessmentResult(
            RequestRiskLevel riskLevel,
            int riskScore,
            List<String> flags
    ) {
        public boolean isFlagged() {
            return riskLevel != RequestRiskLevel.NORMAL;
        }
    }

    @Transactional(readOnly = true)
    public RiskAssessmentResult assessRequest(
            UserAccount requester,
            BloodGroup bloodGroup,
            String hospital,
            String patientReference) {

        int score = 0;
        List<String> flags = new ArrayList<>();
        Instant now = Instant.now();

        // 1. Check duplicate active request by patient reference & blood group
        if (patientReference != null && !patientReference.isBlank()) {
            List<BloodRequest> existingPatientRequests = bloodRequestRepository
                    .findByBloodGroupAndPatientReferenceIgnoreCaseAndStatus(
                            bloodGroup, patientReference.trim(), BloodRequestStatus.OPEN);

            if (!existingPatientRequests.isEmpty()) {
                score += 50;
                flags.add("Duplicate alert: " + existingPatientRequests.size()
                        + " open request(s) already exist for patient ref '" + patientReference.trim()
                        + "' with blood group " + bloodGroup.getDisplay());
            }
        }

        // 2. Rate limit / velocity check for requester (last 15 minutes)
        Instant fifteenMinutesAgo = now.minus(Duration.ofMinutes(15));
        List<BloodRequest> recentRequests = bloodRequestRepository
                .findByRequesterAndCreatedAtAfter(requester, fifteenMinutesAgo);

        if (recentRequests.size() >= 3) {
            score += 35;
            flags.add("Velocity flag: Requester submitted " + recentRequests.size()
                    + " requests within the last 15 minutes");
        }

        // 3. Repeated requests for same hospital & blood group (last 24 hours)
        Instant twentyFourHoursAgo = now.minus(Duration.ofHours(24));
        List<BloodRequest> dayRequests = bloodRequestRepository
                .findByRequesterAndCreatedAtAfter(requester, twentyFourHoursAgo);

        long matchingHospitalCount = dayRequests.stream()
                .filter(r -> r.getBloodGroup() == bloodGroup)
                .filter(r -> r.getHospital() != null && r.getHospital().trim().equalsIgnoreCase(hospital != null ? hospital.trim() : ""))
                .count();

        if (matchingHospitalCount >= 1) {
            score += 20;
            flags.add("Repetitive pattern: " + matchingHospitalCount
                    + " recent request(s) found for same hospital ('" + hospital + "') and blood group " + bloodGroup.getDisplay());
        }

        // Determine final risk level
        RequestRiskLevel riskLevel;
        if (score >= 50) {
            riskLevel = RequestRiskLevel.HIGH_RISK;
        } else if (score >= 20) {
            riskLevel = RequestRiskLevel.REVIEW_REQUIRED;
        } else {
            riskLevel = RequestRiskLevel.NORMAL;
        }

        return new RiskAssessmentResult(riskLevel, score, flags);
    }
}
