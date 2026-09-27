package BloodBridge.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByUserEmailOrderByTimestampDesc(String userEmail);

    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);
}
