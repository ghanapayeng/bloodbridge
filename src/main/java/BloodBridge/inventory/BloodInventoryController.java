package BloodBridge.inventory;

import BloodBridge.common.BloodGroup;
import BloodBridge.inventory.BloodInventoryDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class BloodInventoryController {

    private final BloodInventoryService inventoryService;

    public BloodInventoryController(BloodInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> listInventory(
            @RequestParam(required = false) String facilityName,
            @RequestParam(required = false) InventoryStatus status,
            @RequestParam(required = false) BloodGroup bloodGroup) {
        return ResponseEntity.ok(inventoryService.getInventory(facilityName, status, bloodGroup));
    }

    @GetMapping("/alerts")
    public ResponseEntity<InventoryAlertsResponse> getAlerts() {
        return ResponseEntity.ok(inventoryService.getInventoryAlerts());
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> addBatch(
            Authentication authentication,
            @Valid @RequestBody CreateInventoryRequest request) {
        String email = authentication != null ? authentication.getName() : "system";
        InventoryResponse response = inventoryService.addInventoryBatch(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/reserve")
    public ResponseEntity<InventoryResponse> reserveUnits(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateInventoryQuantityRequest request) {
        String email = authentication != null ? authentication.getName() : "system";
        InventoryResponse response = inventoryService.reserveUnits(id, request.quantity(), email);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/issue")
    public ResponseEntity<InventoryResponse> issueUnits(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateInventoryQuantityRequest request) {
        String email = authentication != null ? authentication.getName() : "system";
        InventoryResponse response = inventoryService.issueUnits(id, request.quantity(), email);
        return ResponseEntity.ok(response);
    }
}
