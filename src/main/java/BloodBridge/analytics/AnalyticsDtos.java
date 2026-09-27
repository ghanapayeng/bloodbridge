package BloodBridge.analytics;

import BloodBridge.common.BloodGroup;
import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record BloodGroupDemandForecast(
            BloodGroup bloodGroup,
            String bloodGroupDisplay,
            int currentInventoryUnits,
            int historical30DayDemandUnits,
            int projectedWeeklyDemandUnits,
            int projectedMonthlyDemandUnits,
            boolean shortageRisk,
            int projectedDeficitUnits,
            String recommendation
    ) {
    }

    public record DemandForecastResponse(
            int totalCurrentInventory,
            int totalHistoricalDemand30Days,
            int totalProjectedMonthlyDemand,
            List<BloodGroupDemandForecast> groupForecasts,
            List<String> systemRecommendations
    ) {
    }
}
