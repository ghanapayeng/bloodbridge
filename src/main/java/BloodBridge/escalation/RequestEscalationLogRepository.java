package BloodBridge.escalation;

import BloodBridge.request.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RequestEscalationLogRepository extends JpaRepository<RequestEscalationLog, Long> {

    List<RequestEscalationLog> findByBloodRequestOrderByEscalatedAtAsc(BloodRequest bloodRequest);

    List<RequestEscalationLog> findByBloodRequest_IdOrderByEscalatedAtAsc(Long requestId);
}
