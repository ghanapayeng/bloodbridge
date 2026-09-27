package BloodBridge.audit;

import java.time.Instant;

public record AdminTelemetryDto(
        long totalUsers,
        long totalDonors,
        long totalRequests,
        long openRequests,
        long emergencyRequests,
        long flaggedRequests,
        long totalDonations,
        int totalInventoryUnitsAvailable,
        long activeTransfers,
        String systemStatus,
        Instant serverTime
) {
}
