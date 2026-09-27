package BloodBridge.escalation;

import BloodBridge.request.BloodRequest;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "request_escalation_logs")
public class RequestEscalationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    @Column(name = "search_radius_km", nullable = false)
    private Double searchRadiusKm;

    @Column(name = "donors_notified_count", nullable = false)
    private Integer donorsNotifiedCount;

    @Column(name = "action_summary", length = 500)
    private String actionSummary;

    @Column(name = "escalated_at", nullable = false, updatable = false)
    private Instant escalatedAt;

    protected RequestEscalationLog() {
    }

    public RequestEscalationLog(
            BloodRequest bloodRequest,
            Double searchRadiusKm,
            Integer donorsNotifiedCount,
            String actionSummary) {
        this.bloodRequest = bloodRequest;
        this.searchRadiusKm = searchRadiusKm;
        this.donorsNotifiedCount = donorsNotifiedCount != null ? donorsNotifiedCount : 0;
        this.actionSummary = actionSummary;
    }

    @PrePersist
    void onCreate() {
        escalatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BloodRequest getBloodRequest() {
        return bloodRequest;
    }

    public Double getSearchRadiusKm() {
        return searchRadiusKm;
    }

    public Integer getDonorsNotifiedCount() {
        return donorsNotifiedCount;
    }

    public String getActionSummary() {
        return actionSummary;
    }

    public Instant getEscalatedAt() {
        return escalatedAt;
    }
}
