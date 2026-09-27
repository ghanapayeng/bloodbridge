package BloodBridge.donor;

import BloodBridge.common.BloodGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class DonorDtos {

    private DonorDtos() {
    }

    public record UpsertDonorProfileRequest(
            @NotNull BloodGroup bloodGroup,
            @NotBlank @Size(max = 30) String phone,
            @NotBlank @Size(max = 120) String city,
            @NotNull Boolean available,
            LocalDate lastDonationDate,
            Double latitude,
            Double longitude) {

        public UpsertDonorProfileRequest(BloodGroup bloodGroup, String phone, String city, Boolean available, LocalDate lastDonationDate) {
            this(bloodGroup, phone, city, available, lastDonationDate, null, null);
        }
    }

    public record MyDonorProfileResponse(
            Long id,
            String fullName,
            String email,
            BloodGroup bloodGroup,
            String phone,
            String city,
            boolean available,
            LocalDate lastDonationDate,
            Double latitude,
            Double longitude,
            EligibilityStatus eligibilityStatus,
            LocalDate nextEligibleDate,
            int totalDonationsCount,
            String donorUuid) {

        public static MyDonorProfileResponse from(DonorProfile profile) {
            return new MyDonorProfileResponse(
                    profile.getId(),
                    profile.getUser().getFullName(),
                    profile.getUser().getEmail(),
                    profile.getBloodGroup(),
                    profile.getPhone(),
                    profile.getCity(),
                    profile.isAvailable(),
                    profile.getLastDonationDate(),
                    profile.getLatitude(),
                    profile.getLongitude(),
                    profile.getEligibilityStatus(),
                    profile.getNextEligibleDate(),
                    profile.getTotalDonationsCount(),
                    profile.getUser().getDonorUuid());
        }
    }

    public record DonorMatchResponse(Long donorId, String fullName, BloodGroup bloodGroup, String city) {

        public static DonorMatchResponse from(DonorProfile profile) {
            return new DonorMatchResponse(
                    profile.getUser().getId(),
                    profile.getUser().getFullName(),
                    profile.getBloodGroup(),
                    profile.getCity());
        }
    }

    public record SmartDonorMatchDto(
            Long donorId,
            String fullName,
            BloodGroup bloodGroup,
            String city,
            int matchScore,
            Double distanceKm,
            String distanceFormatted,
            boolean available,
            boolean eligible,
            EligibilityStatus eligibilityStatus,
            String compatibilityLabel,
            LocalDate lastDonationDate) {
    }

    public record DonorEligibilityResponse(
            Long donorId,
            boolean eligible,
            EligibilityStatus status,
            LocalDate lastDonationDate,
            LocalDate nextEligibleDate,
            long daysRemaining,
            int cooldownDaysRequired,
            String reason,
            String logisticalDisclaimer) {
    }

    public record DonorVerificationResponse(
            boolean verified,
            String donorName,
            BloodGroup bloodGroup,
            EligibilityStatus eligibilityStatus,
            int totalDonations,
            Integer memberSinceYear,
            String donorUuid,
            String verificationMessage) {
    }
}
