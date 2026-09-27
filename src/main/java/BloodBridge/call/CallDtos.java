package BloodBridge.call;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public final class CallDtos {

    private CallDtos() {
    }

    public record SendSignalRequest(
            @NotNull Long requestId,
            Long targetUserId,
            Long recipientId,
            @NotBlank String signalType,
            String signalData) {

        public Long getTargetUserId() {
            if (targetUserId != null) return targetUserId;
            if (recipientId != null) return recipientId;
            throw new IllegalArgumentException("Either targetUserId or recipientId must be provided.");
        }
    }

    public record CallSignalResponse(
            Long id,
            Long requestId,
            Long callerId,
            String callerName,
            Long recipientId,
            String recipientName,
            String signalType,
            String signalData,
            Instant createdAt) {

        public static CallSignalResponse from(CallSignal signal) {
            return new CallSignalResponse(
                    signal.getId(),
                    signal.getBloodRequest().getId(),
                    signal.getCaller().getId(),
                    signal.getCaller().getFullName(),
                    signal.getRecipient().getId(),
                    signal.getRecipient().getFullName(),
                    signal.getSignalType(),
                    signal.getSignalData(),
                    signal.getCreatedAt());
        }
    }
}
