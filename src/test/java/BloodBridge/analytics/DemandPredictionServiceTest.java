package BloodBridge.analytics;

import BloodBridge.analytics.AnalyticsDtos.BloodGroupDemandForecast;
import BloodBridge.analytics.AnalyticsDtos.DemandForecastResponse;
import BloodBridge.analytics.ForecastDtos.*;
import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import BloodBridge.inventory.BloodInventory;
import BloodBridge.inventory.BloodInventoryRepository;
import BloodBridge.inventory.InventoryStatus;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestRepository;
import BloodBridge.request.RequestUrgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandPredictionServiceTest {

    @Mock
    private BloodRequestRepository bloodRequestRepository;

    @Mock
    private BloodInventoryRepository inventoryRepository;

    @Mock
    private BloodDemandRecordRepository demandRecordRepository;

    private DemandPredictionService service;

    @BeforeEach
    void setUp() {
        service = new DemandPredictionService(bloodRequestRepository, inventoryRepository, demandRecordRepository);
    }

    @Test
    void shouldFlagShortageWhenInventoryIsInsufficient() {
        // Stock only 1 unit of O_NEGATIVE
        BloodInventory batch = new BloodInventory(
                BloodGroup.O_NEGATIVE,
                "-",
                BloodComponent.PRBC,
                1,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "BATCH-1",
                "AIIMS",
                InventoryStatus.AVAILABLE,
                false
        );
        when(inventoryRepository.findByStatus(InventoryStatus.AVAILABLE)).thenReturn(List.of(batch));

        DemandForecastResponse response = service.calculateForecast();

        assertThat(response).isNotNull();
        BloodGroupDemandForecast oNegForecast = response.groupForecasts().stream()
                .filter(f -> f.bloodGroup() == BloodGroup.O_NEGATIVE)
                .findFirst()
                .orElseThrow();

        assertThat(oNegForecast.shortageRisk()).isTrue();
        assertThat(oNegForecast.projectedDeficitUnits()).isGreaterThan(0);
        assertThat(response.systemRecommendations()).anyMatch(r -> r.contains("O-"));
    }

    @Test
    void shouldCalculateSummaryWithNationalFiguresAndMultiHorizonForecasts() {
        BloodInventory bPosBatch = new BloodInventory(
                BloodGroup.B_POSITIVE,
                "+",
                BloodComponent.PRBC,
                30,
                LocalDate.now(),
                LocalDate.now().plusDays(35),
                "BATCH-B-POS",
                "Metro Hospital",
                InventoryStatus.AVAILABLE,
                true
        );
        BloodInventory oNegBatch = new BloodInventory(
                BloodGroup.O_NEGATIVE,
                "-",
                BloodComponent.WHOLE_BLOOD,
                2,
                LocalDate.now(),
                LocalDate.now().plusDays(25),
                "BATCH-O-NEG",
                "City Care",
                InventoryStatus.AVAILABLE,
                true
        );
        when(inventoryRepository.findByStatus(InventoryStatus.AVAILABLE)).thenReturn(List.of(bPosBatch, oNegBatch));

        ForecastSummaryResponse summary = service.getForecastSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.nationalFigures()).isNotNull();
        assertThat(summary.nationalFigures().annualEstimatedRequirementMillion()).isEqualTo(14.60);
        assertThat(summary.nationalFigures().projection2026Million()).isEqualTo(15.04);
        assertThat(summary.nationalFigures().projection2030Million()).isEqualTo(16.92);

        assertThat(summary.totalCurrentInventoryUnits()).isEqualTo(32);
        assertThat(summary.forecast7DayDemandUnits()).isGreaterThan(0);
        assertThat(summary.forecast30DayDemandUnits()).isGreaterThan(summary.forecast7DayDemandUnits());
        assertThat(summary.forecast90DayDemandUnits()).isGreaterThan(summary.forecast30DayDemandUnits());
        assertThat(summary.isDemoData()).isTrue();
    }

    @Test
    void shouldCoverAllEightBloodGroupsAndFiveComponents() {
        when(inventoryRepository.findByStatus(InventoryStatus.AVAILABLE)).thenReturn(List.of());

        List<BloodGroupDemandDetail> groups = service.getBloodGroupDemandBreakdown();
        assertThat(groups).hasSize(8);

        List<ComponentDemandDetail> components = service.getComponentDemandBreakdown();
        assertThat(components).hasSize(5);

        List<ShortageAlertDto> shortages = service.getShortageAlerts();
        assertThat(shortages).isNotEmpty();
    }
}
