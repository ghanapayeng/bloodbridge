package BloodBridge.transfer;

import BloodBridge.common.BloodGroup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class BloodTransferDtos {

    public record CreateTransferRequest(
            @NotBlank String sourceHospital,
            @NotBlank String destinationHospital,
            @NotNull BloodGroup bloodGroup,
            @NotNull @Min(1) Integer units,
            String notes
    ) {}

    public record TransferResponse(
            Long id,
            String sourceHospital,
            String destinationHospital,
            BloodGroup bloodGroup,
            int units,
            TransferStatus status,
            String notes,
            String requestedByName,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static TransferResponse fromEntity(BloodTransfer t) {
            String requester = t.getRequestedBy() != null ? t.getRequestedBy().getFullName() : "Facility Coordinator";
            return new TransferResponse(
                    t.getId(),
                    t.getSourceHospital(),
                    t.getDestinationHospital(),
                    t.getBloodGroup(),
                    t.getUnits(),
                    t.getStatus(),
                    t.getNotes(),
                    requester,
                    t.getCreatedAt(),
                    t.getUpdatedAt()
            );
        }
    }

    public record UpdateTransferStatusRequest(
            @NotNull TransferStatus status,
            String trackingNotes
    ) {}
}
