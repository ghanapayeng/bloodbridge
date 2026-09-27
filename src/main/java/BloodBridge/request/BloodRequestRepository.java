package BloodBridge.request;

import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodGroup;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByStatusOrderByCreatedAtDesc(BloodRequestStatus status);

    List<BloodRequest> findByRequesterOrderByCreatedAtDesc(UserAccount requester);

    List<BloodRequest> findByRiskLevelOrderByCreatedAtDesc(RequestRiskLevel riskLevel);

    List<BloodRequest> findByRiskLevelNotOrderByCreatedAtDesc(RequestRiskLevel riskLevel);

    List<BloodRequest> findByRequesterAndCreatedAtAfter(UserAccount requester, Instant since);

    List<BloodRequest> findByBloodGroupAndPatientReferenceIgnoreCaseAndStatus(
            BloodGroup bloodGroup, String patientReference, BloodRequestStatus status);
}
