package BloodBridge.donation;

import BloodBridge.common.BloodGroup;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class DonationDtos {

    private DonationDtos() {
    }

    public record CreateDonation(
            Long bloodRequestId,
            @NotNull @PastOrPresent LocalDate donatedAt,
            @NotNull @DecimalMin("0.1") @DecimalMax("2.0") BigDecimal units) {
    }

    public record DonationResponse(
            Long id,
            Long bloodRequestId,
            LocalDate donatedAt,
            BigDecimal units,
            String hospital,
            String city,
            BloodGroup bloodGroup,
            Instant createdAt) {

        public static DonationResponse from(Donation donation) {
            String hospital = donation.getBloodRequest() != null ? donation.getBloodRequest().getHospital() : null;
            String city = donation.getBloodRequest() != null ? donation.getBloodRequest().getCity() : null;
            BloodGroup bloodGroup = donation.getBloodRequest() != null ? donation.getBloodRequest().getBloodGroup() : null;
            return new DonationResponse(
                    donation.getId(),
                    donation.getBloodRequest() == null ? null : donation.getBloodRequest().getId(),
                    donation.getDonatedAt(),
                    donation.getUnits(),
                    hospital,
                    city,
                    bloodGroup,
                    donation.getCreatedAt());
        }
    }

    public record DonationCertificateDto(
            Long donationId,
            String certificateNumber,
            String donorName,
            BloodGroup bloodGroup,
            BigDecimal unitsDonated,
            LocalDate donationDate,
            String hospital,
            String city,
            String verificationQrUrl
    ) {
    }
}
