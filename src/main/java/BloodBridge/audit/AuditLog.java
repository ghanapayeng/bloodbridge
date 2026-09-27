package BloodBridge.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", length = 254)
    private String userEmail;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "entity_name", length = 64)
    private String entityName;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 32)
    private String result;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    protected AuditLog() {
    }

    public AuditLog(
            String userEmail,
            String action,
            String entityName,
            Long entityId,
            String result,
            String details) {
        this.userEmail = userEmail;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.result = result != null ? result : "SUCCESS";
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getAction() {
        return action;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getResult() {
        return result;
    }

    public String getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
