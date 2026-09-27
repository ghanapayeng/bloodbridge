package BloodBridge.analytics;

import BloodBridge.analytics.AnalyticsDtos.BloodGroupDemandForecast;
import BloodBridge.analytics.AnalyticsDtos.DemandForecastResponse;
import BloodBridge.common.BloodGroup;
import BloodBridge.inventory.BloodInventory;
import BloodBridge.inventory.BloodInventoryRepository;
import BloodBridge.inventory.InventoryStatus;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemandPredictionService {

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodInventoryRepository inventoryRepository;

    public DemandPredictionService(
            BloodRequestRepository bloodRequestRepository,
            BloodInventoryRepository inventoryRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public DemandForecastResponse calculateForecast() {
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        List<BloodRequest> allRequests = bloodRequestRepository.findAll();
        List<BloodInventory> availableInventory = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);

        // Filter requests within last 30 days (or use all if historical dataset is young)
        List<BloodRequest> recentRequests = allRequests.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(thirtyDaysAgo))
                .toList();

        if (recentRequests.isEmpty() && !allRequests.isEmpty()) {
            recentRequests = allRequests;
        }

        List<BloodGroupDemandForecast> forecasts = new ArrayList<>();
        List<String> systemRecommendations = new ArrayList<>();

        int totalInventory = availableInventory.stream().mapToInt(BloodInventory::getQuantity).sum();
        int totalHistorical30d = 0;
        int totalProjectedMonthly = 0;

        for (BloodGroup bg : BloodGroup.values()) {
            // Calculate historical units requested for this blood group
            int pastDemand = recentRequests.stream()
                    .filter(r -> r.getBloodGroup() == bg)
                    .mapToInt(r -> r.getUnitsNeeded() != null ? r.getUnitsNeeded() : 1)
                    .sum();

            totalHistorical30d += pastDemand;

            // Current available units for this blood group
            int currentStock = availableInventory.stream()
                    .filter(i -> i.getBloodGroup() == bg)
                    .mapToInt(BloodInventory::getQuantity)
                    .sum();

            // Forecasting model: rolling 30-day baseline with baseline safety floor (at least 1-2 units/week expected)
            double dailyRate = Math.max(pastDemand / 30.0, 0.2); // minimum baseline demand
            int projectedWeekly = (int) Math.ceil(dailyRate * 7.0);
            int projectedMonthly = (int) Math.ceil(dailyRate * 30.0);
            totalProjectedMonthly += projectedMonthly;

            boolean shortageRisk = currentStock < projectedWeekly;
            int deficit = shortageRisk ? (projectedWeekly - currentStock) : 0;

            String recommendation;
            if (shortageRisk) {
                recommendation = "CRITICAL DEFICIT: Current inventory of " + currentStock + " unit(s) cannot meet the 7-day projected demand of "
                        + projectedWeekly + " units. Immediate donor callout recommended for " + bg.getDisplay() + ".";
                systemRecommendations.add("Priority Alert: Organize urgent blood drive for " + bg.getDisplay()
                        + " (Est. 7-day deficit: " + deficit + " units).");
            } else if (currentStock < projectedMonthly) {
                recommendation = "MODERATE: Stock meets short-term needs (" + currentStock + "/" + projectedWeekly
                        + " weekly units), but falls below 30-day target (" + projectedMonthly + " units).";
            } else {
                recommendation = "OPTIMAL: Safe reserve margin. Current inventory (" + currentStock
                        + " units) comfortably exceeds 30-day projection (" + projectedMonthly + " units).";
            }

            forecasts.add(new BloodGroupDemandForecast(
                    bg,
                    bg.getDisplay(),
                    currentStock,
                    pastDemand,
                    projectedWeekly,
                    projectedMonthly,
                    shortageRisk,
                    deficit,
                    recommendation
            ));
        }

        if (systemRecommendations.isEmpty()) {
            systemRecommendations.add("Inventory levels across all blood groups are currently operating within safe baseline margins.");
        }

        return new DemandForecastResponse(
                totalInventory,
                totalHistorical30d,
                totalProjectedMonthly,
                forecasts,
                systemRecommendations
        );
    }
}
