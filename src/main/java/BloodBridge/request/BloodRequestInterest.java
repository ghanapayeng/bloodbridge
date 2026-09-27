package BloodBridge.request;

import BloodBridge.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "blood_request_interests")
public class BloodRequestInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private UserAccount donor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BloodRequestInterestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BloodRequestInterest() {
    }

    public BloodRequestInterest(BloodRequest bloodRequest, UserAccount donor) {
        this.bloodRequest = bloodRequest;
        this.donor = donor;
        this.status = BloodRequestInterestStatus.PENDING;
    }

    public void setStatus(BloodRequestInterestStatus status) {
        this.status = status;
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

    public UserAccount getDonor() {
        return donor;
    }

    public BloodRequestInterestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
