package BloodBridge.donation;

import BloodBridge.auth.UserAccount;
import BloodBridge.request.BloodRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private UserAccount donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blood_request_id")
    private BloodRequest bloodRequest;

    @Column(name = "donated_at", nullable = false)
    private LocalDate donatedAt;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal units;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Donation() {
    }

    public Donation(UserAccount donor, BloodRequest bloodRequest, LocalDate donatedAt, BigDecimal units) {
        this.donor = donor;
        this.bloodRequest = bloodRequest;
        this.donatedAt = donatedAt;
        this.units = units;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getDonor() {
        return donor;
    }

    public BloodRequest getBloodRequest() {
        return bloodRequest;
    }

    public LocalDate getDonatedAt() {
        return donatedAt;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
