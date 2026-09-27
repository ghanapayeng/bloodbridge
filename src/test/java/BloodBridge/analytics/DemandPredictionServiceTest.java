package BloodBridge.analytics;

import BloodBridge.analytics.AnalyticsDtos.BloodGroupDemandForecast;
import BloodBridge.analytics.AnalyticsDtos.DemandForecastResponse;
import BloodBridge.auth.UserAccount;
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

    private DemandPredictionService service;

    @BeforeEach
    void setUp() {
        service = new DemandPredictionService(bloodRequestRepository, inventoryRepository);
    }

    @Test
    void shouldFlagShortageWhenInventoryIsInsufficient() {
        UserAccount requester = new UserAccount("Requester", "req@example.com", "hash", "ROLE_USER");
        BloodRequest req = new BloodRequest(requester, BloodGroup.O_NEGATIVE, "Delhi", "AIIMS", RequestUrgency.URGENT, null, 10, null, null, null, null);
        ReflectionTestUtils.setField(req, "createdAt", Instant.now());

        when(bloodRequestRepository.findAll()).thenReturn(List.of(req));

        // Only 1 available unit in stock for O_NEGATIVE
        BloodInventory batch = new BloodInventory(BloodGroup.O_NEGATIVE, "-", 1, LocalDate.now(), LocalDate.now().plusDays(30), "BATCH-1", "AIIMS", InventoryStatus.AVAILABLE);
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
}
