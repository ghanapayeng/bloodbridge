package BloodBridge.analytics;

import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ForecastDtos {

    private ForecastDtos() {
    }

    public record NationalPlanningFigures(
            double annualEstimatedRequirementMillion,
            double projection2026Million,
            double projection2027Million,
            double projection2028Million,
            double projection2029Million,
            double projection2030Million,
            String populationReferenceBasis,
            String referenceDisclaimer
    ) {
        public static NationalPlanningFigures createDefault() {
            return new NationalPlanningFigures(
                    14.60,
                    15.04,
                    15.49,
                    15.95,
                    16.43,
                    16.92,
                    "Estimated national blood requirement in India (~1% of population baseline as recommended by WHO)",
                    "Illustrative academic projections for strategic planning demonstration, not official government forecasts."
            );
        }
    }

    public record ForecastSummaryResponse(
            NationalPlanningFigures nationalFigures,
            int totalCurrentInventoryUnits,
            int historical30DayDemandUnits,
            int forecast7DayDemandUnits,
            int forecast30DayDemandUnits,
            int forecast90DayDemandUnits,
            int activeShortageAlertsCount,
            String highestDemandBloodGroup,
            String highestDemandBloodGroupDisplay,
            String highestDemandComponent,
            List<String> operationalRecommendations,
            boolean isDemoData,
            String modelDescription
    ) {}

    public record MonthlyDemandTrendDto(
            int year,
            int month,
            String monthLabel,
            int demandUnits,
            int suppliedUnits,
            boolean isForecast,
            String seasonalContext
    ) {}

    public record BloodGroupDemandDetail(
            BloodGroup bloodGroup,
            String bloodGroupDisplay,
            double populationPrevalencePercent,
            int currentInventoryUnits,
            int historical30DayDemandUnits,
            int forecast7DayDemandUnits,
            int forecast30DayDemandUnits,
            int forecast90DayDemandUnits,
            int deficit7DayUnits,
            int deficit30DayUnits,
            String shortageSeverity, // CRITICAL_DEFICIT, MODERATE_DEFICIT, ADEQUATE
            String recommendation,
            Map<String, Integer> componentStock
    ) {}

    public record ComponentDemandDetail(
            BloodComponent component,
            String componentName,
            String componentCode,
            int shelfLifeDays,
            int currentInventoryUnits,
            int forecast7DayDemandUnits,
            int forecast30DayDemandUnits,
            int forecast90DayDemandUnits,
            int safeBufferThresholdUnits,
            boolean shortageRisk,
            String clinicalIndications
    ) {}

    public record ShortageAlertDto(
            String id,
            String severity, // CRITICAL, MODERATE, ADVISORY
            BloodGroup bloodGroup,
            String bloodGroupDisplay,
            BloodComponent component,
            String componentName,
            int currentStockUnits,
            int projectedDemand7dUnits,
            int projectedDeficitUnits,
            String headline,
            String actionRequired,
            LocalDate projectedDeficitDate
    ) {}
}
