package BloodBridge.transfer;

import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.Instant;

@Entity
@Table(name = "blood_transfers")
public class BloodTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_hospital", nullable = false, length = 180)
    private String sourceHospital;

    @Column(name = "destination_hospital", nullable = false, length = 180)
    private String destinationHospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 16)
    private BloodGroup bloodGroup;

    @Min(1)
    @Column(nullable = false)
    private Integer units;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransferStatus status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private UserAccount requestedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BloodTransfer() {
    }

    public BloodTransfer(
            String sourceHospital,
            String destinationHospital,
            BloodGroup bloodGroup,
            Integer units,
            String notes,
            UserAccount requestedBy) {
        this.sourceHospital = sourceHospital;
        this.destinationHospital = destinationHospital;
        this.bloodGroup = bloodGroup;
        this.units = units != null ? units : 1;
        this.notes = notes;
        this.requestedBy = requestedBy;
        this.status = TransferStatus.REQUESTED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = TransferStatus.REQUESTED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSourceHospital() {
        return sourceHospital;
    }

    public String getDestinationHospital() {
        return destinationHospital;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public Integer getUnits() {
        return units;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UserAccount getRequestedBy() {
        return requestedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
