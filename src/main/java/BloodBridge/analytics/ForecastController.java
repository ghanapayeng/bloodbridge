package BloodBridge.analytics;

import BloodBridge.analytics.ForecastDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/forecast", "/api/forecast"})
public class ForecastController {

    private final DemandPredictionService demandPredictionService;

    public ForecastController(DemandPredictionService demandPredictionService) {
        this.demandPredictionService = demandPredictionService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ForecastSummaryResponse> getSummary() {
        return ResponseEntity.ok(demandPredictionService.getForecastSummary());
    }

    @GetMapping("/demand")
    public ResponseEntity<List<MonthlyDemandTrendDto>> getDemandTrends() {
        return ResponseEntity.ok(demandPredictionService.getMonthlyDemandTrends());
    }

    @GetMapping("/blood-groups")
    public ResponseEntity<List<BloodGroupDemandDetail>> getBloodGroups() {
        return ResponseEntity.ok(demandPredictionService.getBloodGroupDemandBreakdown());
    }

    @GetMapping("/components")
    public ResponseEntity<List<ComponentDemandDetail>> getComponents() {
        return ResponseEntity.ok(demandPredictionService.getComponentDemandBreakdown());
    }

    @GetMapping("/shortages")
    public ResponseEntity<List<ShortageAlertDto>> getShortages() {
        return ResponseEntity.ok(demandPredictionService.getShortageAlerts());
    }
}
