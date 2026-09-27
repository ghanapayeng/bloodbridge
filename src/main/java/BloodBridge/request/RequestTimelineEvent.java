package BloodBridge.request;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "request_timeline_events")
public class RequestTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestStage stage;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "performed_by", length = 120)
    private String performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequestTimelineEvent() {
    }

    public RequestTimelineEvent(BloodRequest bloodRequest, RequestStage stage, String description, String performedBy) {
        this.bloodRequest = bloodRequest;
        this.stage = stage;
        this.description = description;
        this.performedBy = performedBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BloodRequest getBloodRequest() {
        return bloodRequest;
    }

    public RequestStage getStage() {
        return stage;
    }

    public String getDescription() {
        return description;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
