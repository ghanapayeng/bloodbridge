package BloodBridge.donor;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "donor_profiles")
public class DonorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 16)
    private BloodGroup bloodGroup;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_status", nullable = false, length = 32)
    private EligibilityStatus eligibilityStatus = EligibilityStatus.ELIGIBLE;

    @Column(name = "next_eligible_date")
    private LocalDate nextEligibleDate;

    @Column(name = "total_donations_count", nullable = false)
    private int totalDonationsCount = 0;

    @Column(name = "response_count", nullable = false)
    private int responseCount = 0;

    @Column(name = "positive_response_count", nullable = false)
    private int positiveResponseCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DonorProfile() {
    }

    public DonorProfile(UserAccount user) {
        this.user = user;
    }

    public void update(BloodGroup bloodGroup, String phone, String city, boolean available, LocalDate lastDonationDate) {
        update(bloodGroup, phone, city, available, lastDonationDate, this.latitude, this.longitude);
    }

    public void update(
            BloodGroup bloodGroup,
            String phone,
            String city,
            boolean available,
            LocalDate lastDonationDate,
            Double latitude,
            Double longitude) {
        this.bloodGroup = bloodGroup;
        this.phone = phone;
        this.city = city;
        this.available = available;
        this.lastDonationDate = lastDonationDate;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setEligibilityStatus(EligibilityStatus eligibilityStatus) {
        this.eligibilityStatus = eligibilityStatus;
    }

    public void setNextEligibleDate(LocalDate nextEligibleDate) {
        this.nextEligibleDate = nextEligibleDate;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setTotalDonationsCount(int totalDonationsCount) {
        this.totalDonationsCount = totalDonationsCount;
    }

    public void incrementDonationsCount() {
        this.totalDonationsCount++;
    }

    public void incrementResponseCount(boolean positive) {
        this.responseCount++;
        if (positive) {
            this.positiveResponseCount++;
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (eligibilityStatus == null) {
            eligibilityStatus = EligibilityStatus.ELIGIBLE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public boolean isAvailable() {
        return available;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public EligibilityStatus getEligibilityStatus() {
        return eligibilityStatus != null ? eligibilityStatus : EligibilityStatus.ELIGIBLE;
    }

    public LocalDate getNextEligibleDate() {
        return nextEligibleDate;
    }

    public int getTotalDonationsCount() {
        return totalDonationsCount;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public int getPositiveResponseCount() {
        return positiveResponseCount;
    }
}
