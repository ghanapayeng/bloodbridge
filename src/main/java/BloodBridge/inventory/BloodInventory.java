package BloodBridge.inventory;

import BloodBridge.common.BloodGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "blood_inventories")
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 16)
    private BloodGroup bloodGroup;

    @Column(name = "rh_type", nullable = false, length = 8)
    private String rhType;

    @Min(0)
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "batch_unit_id", nullable = false, unique = true, length = 64)
    private String batchUnitId;

    @Column(name = "facility_name", nullable = false, length = 180)
    private String facilityName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InventoryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BloodInventory() {
    }

    public BloodInventory(
            BloodGroup bloodGroup,
            String rhType,
            Integer quantity,
            LocalDate collectionDate,
            LocalDate expiryDate,
            String batchUnitId,
            String facilityName,
            InventoryStatus status) {
        this.bloodGroup = bloodGroup;
        this.rhType = rhType != null ? rhType : (bloodGroup.name().contains("POSITIVE") ? "+" : "-");
        this.quantity = (quantity != null && quantity >= 0) ? quantity : 0;
        this.collectionDate = collectionDate;
        this.expiryDate = expiryDate;
        this.batchUnitId = batchUnitId;
        this.facilityName = facilityName;
        this.status = status != null ? status : InventoryStatus.AVAILABLE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (quantity == null || quantity < 0) {
            quantity = 0;
        }
        if (status == null) {
            status = InventoryStatus.AVAILABLE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public String getRhType() {
        return rhType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Inventory quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBatchUnitId() {
        return batchUnitId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public InventoryStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
