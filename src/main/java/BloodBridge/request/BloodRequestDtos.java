package BloodBridge.request;

import BloodBridge.common.BloodGroup;
import BloodBridge.donor.DonorProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class BloodRequestDtos {

    private BloodRequestDtos() {
    }

    public record CreateBloodRequest(
            @NotNull BloodGroup bloodGroup,
            @NotBlank @Size(max = 120) String city,
            @NotBlank @Size(max = 180) String hospital,
            @NotNull RequestUrgency urgency,
            @Size(max = 2000) String additionalMessage,
            Integer unitsNeeded,
            String patientReference,
            Instant requiredBy,
            Double latitude,
            Double longitude) {

        public CreateBloodRequest(
                BloodGroup bloodGroup,
                String city,
                String hospital,
                RequestUrgency urgency,
                String additionalMessage) {
            this(bloodGroup, city, hospital, urgency, additionalMessage, 1, null, null, null, null);
        }
    }

    public record UpdateRequestStatus(@NotNull BloodRequestStatus status) {
    }

    public record UpdateInterestStatus(@NotNull BloodRequestInterestStatus status) {
    }

    public record UpdateFulfillmentRequest(int unitsFulfilled) {
    }

    public record RiskReviewRequest(boolean approved, String notes) {
    }

    public record BloodRequestResponse(
            Long id,
            Long requesterId,
            String requesterName,
            BloodGroup bloodGroup,
            String city,
            String hospital,
            RequestUrgency urgency,
            BloodRequestStatus status,
            String additionalMessage,
            int unitsNeeded,
            int unitsFulfilled,
            String patientReference,
            Instant requiredBy,
            Double latitude,
            Double longitude,
            RequestRiskLevel riskLevel,
            RequestStage currentStage,
            boolean emergency,
            Instant createdAt) {

        public static BloodRequestResponse from(BloodRequest request) {
            return new BloodRequestResponse(
                    request.getId(),
                    request.getRequester().getId(),
                    request.getRequester().getFullName(),
                    request.getBloodGroup(),
                    request.getCity(),
                    request.getHospital(),
                    request.getUrgency(),
                    request.getStatus(),
                    request.getAdditionalMessage(),
                    request.getUnitsNeeded(),
                    request.getUnitsFulfilled(),
                    request.getPatientReference(),
                    request.getRequiredBy(),
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getRiskLevel(),
                    request.getCurrentStage(),
                    request.getUrgency() != null && request.getUrgency().isEmergency(),
                    request.getCreatedAt());
        }
    }

    public record TimelineEventResponse(
            Long id,
            Long requestId,
            RequestStage stage,
            String description,
            String performedBy,
            Instant createdAt) {

        public static TimelineEventResponse from(RequestTimelineEvent event) {
            return new TimelineEventResponse(
                    event.getId(),
                    event.getBloodRequest().getId(),
                    event.getStage(),
                    event.getDescription(),
                    event.getPerformedBy(),
                    event.getCreatedAt());
        }
    }

    public record DonorInterestResponse(
            Long id,
            Long donorId,
            String donorName,
            String donorEmail,
            String donorPhone,
            BloodGroup bloodGroup,
            String city,
            BloodRequestInterestStatus status,
            Instant createdAt) {

        public static DonorInterestResponse from(BloodRequestInterest interest, DonorProfile donorProfile) {
            return new DonorInterestResponse(
                    interest.getId(),
                    interest.getDonor().getId(),
                    interest.getDonor().getFullName(),
                    interest.getDonor().getEmail(),
                    donorProfile.getPhone(),
                    donorProfile.getBloodGroup(),
                    donorProfile.getCity(),
                    interest.getStatus(),
                    interest.getCreatedAt());
        }
    }

    public record MyDonorInterestResponse(
            Long id,
            Long requestId,
            Long requesterId,
            BloodGroup bloodGroup,
            String city,
            String hospital,
            RequestUrgency urgency,
            BloodRequestStatus requestStatus,
            BloodRequestInterestStatus interestStatus,
            String requesterName,
            Instant createdAt) {

        public static MyDonorInterestResponse from(BloodRequestInterest interest) {
            BloodRequest req = interest.getBloodRequest();
            return new MyDonorInterestResponse(
                    interest.getId(),
                    req.getId(),
                    req.getRequester().getId(),
                    req.getBloodGroup(),
                    req.getCity(),
                    req.getHospital(),
                    req.getUrgency(),
                    req.getStatus(),
                    interest.getStatus(),
                    req.getRequester().getFullName(),
                    interest.getCreatedAt());
        }
    }
}

