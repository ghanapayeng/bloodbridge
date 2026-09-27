package BloodBridge.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog logAction(
            String userEmail,
            String action,
            String entityName,
            Long entityId,
            String result,
            String details) {
        AuditLog log = new AuditLog(userEmail, action, entityName, entityId, result, details);
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findAllByOrderByTimestampDesc().stream()
                .limit(limit > 0 ? limit : 50)
                .toList();
    }
}
