package BloodBridge.inventory;

import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import BloodBridge.inventory.BloodInventoryDtos.*;
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
class BloodInventoryControllerTest {

    @Mock
    private BloodInventoryService inventoryService;

    private BloodInventoryController controller;

    @BeforeEach
    void setUp() {
        controller = new BloodInventoryController(inventoryService);
    }

    @Test
    void shouldReturnInventorySummary() {
        InventorySummaryResponse mockSummary = new InventorySummaryResponse(
                45,
                6,
                10,
                3,
                2,
                1,
                List.of(new BloodGroupStockDto(BloodGroup.B_POSITIVE, "B+", 32, false, false, Map.of("PRBC", 32))),
                List.of(new ComponentStockDto(BloodComponent.PRBC, "PRBC", "PRBC", 32, 42)),
                true,
                "DEMO DATA"
        );

        when(inventoryService.getInventorySummary()).thenReturn(mockSummary);

        ResponseEntity<InventorySummaryResponse> response = controller.getSummary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalAvailableUnits()).isEqualTo(45);
        assertThat(response.getBody().hasDemoData()).isTrue();
    }

    @Test
    void shouldListInventoryWithFilters() {
        InventoryResponse item = new InventoryResponse(
                1L,
                BloodGroup.O_POSITIVE,
                "+",
                BloodComponent.PRBC,
                24,
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(37),
                "DEMO-1",
                "Apex National Blood Center",
                InventoryStatus.AVAILABLE,
                true,
                37,
                false,
                false
        );

        when(inventoryService.getInventory("Apex National Blood Center", InventoryStatus.AVAILABLE, BloodGroup.O_POSITIVE, BloodComponent.PRBC))
                .thenReturn(List.of(item));

        ResponseEntity<List<InventoryResponse>> response = controller.listInventory(
                "Apex National Blood Center", InventoryStatus.AVAILABLE, BloodGroup.O_POSITIVE, BloodComponent.PRBC
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).facilityName()).isEqualTo("Apex National Blood Center");
        assertThat(response.getBody().get(0).component()).isEqualTo(BloodComponent.PRBC);
    }
}
