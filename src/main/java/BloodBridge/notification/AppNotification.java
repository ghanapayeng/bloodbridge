package BloodBridge.notification;

import BloodBridge.auth.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_notifications")
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "related_request_id")
    private Long relatedRequestId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppNotification() {
    }

    public AppNotification(
            UserAccount recipient,
            NotificationType type,
            String title,
            String message,
            Long relatedRequestId) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedRequestId = relatedRequestId;
        this.isRead = false;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getRecipient() {
        return recipient;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Long getRelatedRequestId() {
        return relatedRequestId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
