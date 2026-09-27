package BloodBridge.escalation;

import java.time.Instant;

public class EscalationDtos {

    public record EscalationLogResponse(
            Long id,
            Long requestId,
            Double searchRadiusKm,
            Integer donorsNotifiedCount,
            String actionSummary,
            Instant escalatedAt
    ) {
        public static EscalationLogResponse fromEntity(RequestEscalationLog log) {
            return new EscalationLogResponse(
                    log.getId(),
                    log.getBloodRequest().getId(),
                    log.getSearchRadiusKm(),
                    log.getDonorsNotifiedCount(),
                    log.getActionSummary(),
                    log.getEscalatedAt()
            );
        }
    }

    public record EscalationResultResponse(
            Long requestId,
            int stepNumber,
            Double currentRadiusKm,
            Double nextRadiusKm,
            int newDonorsNotified,
            int totalDonorsNotified,
            boolean hospitalLevelEscalated,
            String summary
    ) {}
}
