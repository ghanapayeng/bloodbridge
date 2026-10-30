package BloodBridge.analytics;

import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "blood_demand_records", indexes = {
        @Index(name = "idx_bdr_period", columnList = "period_year, period_month"),
        @Index(name = "idx_bdr_bg_comp", columnList = "blood_group, component"),
        @Index(name = "idx_bdr_forecast", columnList = "is_forecast")
})
public class BloodDemandRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 16)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "component", nullable = false, length = 32)
    private BloodComponent component;

    @Column(name = "units_demanded", nullable = false)
    private int unitsDemanded;

    @Column(name = "units_supplied", nullable = false)
    private int unitsSupplied;

    @Column(name = "is_forecast", nullable = false)
    private boolean isForecast;

    @Column(name = "facility_name", nullable = false, length = 180)
    private String facilityName;

    @Column(length = 255)
    private String notes;

    @Column(name = "is_demo", nullable = false)
    private boolean isDemo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BloodDemandRecord() {
    }

    public BloodDemandRecord(
            int periodYear,
            int periodMonth,
            LocalDate periodDate,
            BloodGroup bloodGroup,
            BloodComponent component,
            int unitsDemanded,
            int unitsSupplied,
            boolean isForecast,
            String facilityName,
            String notes,
            boolean isDemo) {
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.periodDate = periodDate;
        this.bloodGroup = bloodGroup;
        this.component = component != null ? component : BloodComponent.WHOLE_BLOOD;
        this.unitsDemanded = Math.max(0, unitsDemanded);
        this.unitsSupplied = Math.max(0, unitsSupplied);
        this.isForecast = isForecast;
        this.facilityName = facilityName;
        this.notes = notes;
        this.isDemo = isDemo;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (component == null) {
            component = BloodComponent.WHOLE_BLOOD;
        }
    }

    public Long getId() {
        return id;
    }

    public int getPeriodYear() {
        return periodYear;
    }

    public int getPeriodMonth() {
        return periodMonth;
    }

    public LocalDate getPeriodDate() {
        return periodDate;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public BloodComponent getComponent() {
        return component;
    }

    public int getUnitsDemanded() {
        return unitsDemanded;
    }

    public void setUnitsDemanded(int unitsDemanded) {
        this.unitsDemanded = unitsDemanded;
    }

    public int getUnitsSupplied() {
        return unitsSupplied;
    }

    public void setUnitsSupplied(int unitsSupplied) {
        this.unitsSupplied = unitsSupplied;
    }

    public boolean isForecast() {
        return isForecast;
    }

    public void setForecast(boolean forecast) {
        isForecast = forecast;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isDemo() {
        return isDemo;
    }

    public void setDemo(boolean demo) {
        isDemo = demo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
