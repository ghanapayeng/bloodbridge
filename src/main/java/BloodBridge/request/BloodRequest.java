package BloodBridge.request;

import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodGroup;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserAccount requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 16)
    private BloodGroup bloodGroup;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 180)
    private String hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RequestUrgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BloodRequestStatus status;

    @Column(name = "additional_message", length = 2000)
    private String additionalMessage;

    @Column(name = "units_needed", nullable = false)
    private Integer unitsNeeded = 1;

    @Column(name = "units_fulfilled", nullable = false)
    private Integer unitsFulfilled = 0;

    @Column(name = "patient_reference", length = 64)
    private String patientReference;

    @Column(name = "required_by")
    private Instant requiredBy;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RequestRiskLevel riskLevel = RequestRiskLevel.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 32)
    private RequestStage currentStage = RequestStage.REQUEST_CREATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BloodRequest() {
    }

    public BloodRequest(
            UserAccount requester,
            BloodGroup bloodGroup,
            String city,
            String hospital,
            RequestUrgency urgency,
            String additionalMessage) {
        this(requester, bloodGroup, city, hospital, urgency, additionalMessage, 1, null, null, null, null);
    }

    public BloodRequest(
            UserAccount requester,
            BloodGroup bloodGroup,
            String city,
            String hospital,
            RequestUrgency urgency,
            String additionalMessage,
            Integer unitsNeeded,
            String patientReference,
            Instant requiredBy,
            Double latitude,
            Double longitude) {
        this.requester = requester;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.hospital = hospital;
        this.urgency = urgency;
        this.status = BloodRequestStatus.OPEN;
        this.additionalMessage = additionalMessage;
        this.unitsNeeded = (unitsNeeded == null || unitsNeeded < 1) ? 1 : unitsNeeded;
        this.unitsFulfilled = 0;
        this.patientReference = patientReference;
        this.requiredBy = requiredBy;
        this.latitude = latitude;
        this.longitude = longitude;
        this.riskLevel = RequestRiskLevel.NORMAL;
        this.currentStage = RequestStage.REQUEST_CREATED;
    }

    public void setStatus(BloodRequestStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setCurrentStage(RequestStage currentStage) {
        this.currentStage = currentStage;
        this.updatedAt = Instant.now();
    }

    public void setRiskLevel(RequestRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
        this.updatedAt = Instant.now();
    }

    public void setUnitsFulfilled(Integer unitsFulfilled) {
        this.unitsFulfilled = unitsFulfilled;
        this.updatedAt = Instant.now();
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setRequiredBy(Instant requiredBy) {
        this.requiredBy = requiredBy;
    }

    public void setPatientReference(String patientReference) {
        this.patientReference = patientReference;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (unitsNeeded == null || unitsNeeded < 1) {
            unitsNeeded = 1;
        }
        if (unitsFulfilled == null) {
            unitsFulfilled = 0;
        }
        if (riskLevel == null) {
            riskLevel = RequestRiskLevel.NORMAL;
        }
        if (currentStage == null) {
            currentStage = RequestStage.REQUEST_CREATED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getRequester() {
        return requester;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public String getCity() {
        return city;
    }

    public String getHospital() {
        return hospital;
    }

    public RequestUrgency getUrgency() {
        return urgency;
    }

    public BloodRequestStatus getStatus() {
        return status;
    }

    public String getAdditionalMessage() {
        return additionalMessage;
    }

    public Integer getUnitsNeeded() {
        return unitsNeeded != null ? unitsNeeded : 1;
    }

    public Integer getUnitsFulfilled() {
        return unitsFulfilled != null ? unitsFulfilled : 0;
    }

    public String getPatientReference() {
        return patientReference;
    }

    public Instant getRequiredBy() {
        return requiredBy;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public RequestRiskLevel getRiskLevel() {
        return riskLevel != null ? riskLevel : RequestRiskLevel.NORMAL;
    }

    public RequestStage getCurrentStage() {
        return currentStage != null ? currentStage : RequestStage.REQUEST_CREATED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
