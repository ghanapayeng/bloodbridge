package BloodBridge.analytics;

import BloodBridge.analytics.ForecastDtos.*;
import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastControllerTest {

    @Mock
    private DemandPredictionService demandPredictionService;

    private ForecastController controller;

    @BeforeEach
    void setUp() {
        controller = new ForecastController(demandPredictionService);
    }

    @Test
    void shouldReturnForecastSummary() {
        ForecastSummaryResponse mockSummary = new ForecastSummaryResponse(
                ForecastDtos.NationalPlanningFigures.createDefault(),
                120,
                350,
                80,
                360,
                1100,
                2,
                "B_POSITIVE",
                "B+",
                "PRBC",
                List.of("Organize blood drive for O-"),
                true,
                "Demo time-series model"
        );

        when(demandPredictionService.getForecastSummary()).thenReturn(mockSummary);

        ResponseEntity<ForecastSummaryResponse> response = controller.getSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().nationalFigures().annualEstimatedRequirementMillion()).isEqualTo(14.60);
        assertThat(response.getBody().nationalFigures().projection2026Million()).isEqualTo(15.04);
        assertThat(response.getBody().nationalFigures().projection2030Million()).isEqualTo(16.92);
        assertThat(response.getBody().highestDemandBloodGroupDisplay()).isEqualTo("B+");
        assertThat(response.getBody().isDemoData()).isTrue();
    }

    @Test
    void shouldReturnMonthlyDemandTrends() {
        List<MonthlyDemandTrendDto> mockTrends = List.of(
                new MonthlyDemandTrendDto(2026, 1, "Jan 2026", 520, 500, false, "Baseline"),
                new MonthlyDemandTrendDto(2026, 2, "Feb 2026", 540, 520, false, "Baseline"),
                new MonthlyDemandTrendDto(2026, 8, "Aug 2026", 720, 0, true, "Monsoon dengue surge")
        );

        when(demandPredictionService.getMonthlyDemandTrends()).thenReturn(mockTrends);

        ResponseEntity<List<MonthlyDemandTrendDto>> response = controller.getDemandTrends();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().get(2).isForecast()).isTrue();
        assertThat(response.getBody().get(2).seasonalContext()).contains("Monsoon");
    }

    @Test
    void shouldReturnBloodGroupsDemand() {
        BloodGroupDemandDetail bPos = new BloodGroupDemandDetail(
                BloodGroup.B_POSITIVE, "B+", 33.0, 32, 215, 52, 215, 660, 0, 183, "OPTIMAL", "Adequate stock", Map.of("PRBC", 20)
        );
        BloodGroupDemandDetail oNeg = new BloodGroupDemandDetail(
                BloodGroup.O_NEGATIVE, "O-", 2.5, 2, 18, 5, 18, 55, 3, 16, "CRITICAL_DEFICIT", "Immediate drive needed", Map.of("PRBC", 2)
        );

        when(demandPredictionService.getBloodGroupDemandBreakdown()).thenReturn(List.of(bPos, oNeg));

        ResponseEntity<List<BloodGroupDemandDetail>> response = controller.getBloodGroups();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).bloodGroupDisplay()).isEqualTo("B+");
        assertThat(response.getBody().get(1).shortageSeverity()).isEqualTo("CRITICAL_DEFICIT");
    }

    @Test
    void shouldReturnComponentsDemand() {
        ComponentDemandDetail prbc = new ComponentDemandDetail(
                BloodComponent.PRBC, "PRBC", "PRBC", 42, 60, 65, 280, 850, 40, false, "Acute blood loss"
        );

        when(demandPredictionService.getComponentDemandBreakdown()).thenReturn(List.of(prbc));

        ResponseEntity<List<ComponentDemandDetail>> response = controller.getComponents();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).componentName()).isEqualTo("PRBC");
        assertThat(response.getBody().get(0).shelfLifeDays()).isEqualTo(42);
    }

    @Test
    void shouldReturnShortages() {
        ShortageAlertDto alert = new ShortageAlertDto(
                "ALERT-1",
                "CRITICAL",
                BloodGroup.O_NEGATIVE,
                "O-",
                BloodComponent.PRBC,
                "PRBC",
                2,
                5,
                3,
                "CRITICAL DEFICIT",
                "Urgent donor drive",
                LocalDate.now().plusDays(3)
        );

        when(demandPredictionService.getShortageAlerts()).thenReturn(List.of(alert));

        ResponseEntity<List<ShortageAlertDto>> response = controller.getShortages();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).severity()).isEqualTo("CRITICAL");
        assertThat(response.getBody().get(0).bloodGroupDisplay()).isEqualTo("O-");
    }
}
