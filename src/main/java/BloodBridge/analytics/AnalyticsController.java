package BloodBridge.analytics;

import BloodBridge.analytics.AnalyticsDtos.DemandForecastResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final DemandPredictionService demandPredictionService;

    public AnalyticsController(DemandPredictionService demandPredictionService) {
        this.demandPredictionService = demandPredictionService;
    }

    @GetMapping("/demand-forecast")
    public ResponseEntity<DemandForecastResponse> getDemandForecast() {
        return ResponseEntity.ok(demandPredictionService.calculateForecast());
    }
}
