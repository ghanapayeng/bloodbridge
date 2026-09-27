package BloodBridge.inventory;

import BloodBridge.common.BloodGroup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BloodInventoryDtos {

    public record CreateInventoryRequest(
            @NotNull BloodGroup bloodGroup,
            String rhType,
            @NotNull @Min(1) Integer quantity,
            @NotNull LocalDate collectionDate,
            @NotNull LocalDate expiryDate,
            String batchUnitId,
            @NotBlank String facilityName
    ) {}

    public record InventoryResponse(
            Long id,
            BloodGroup bloodGroup,
            String rhType,
            int quantity,
            LocalDate collectionDate,
            LocalDate expiryDate,
            String batchUnitId,
            String facilityName,
            InventoryStatus status,
            long daysUntilExpiry,
            boolean expiringSoon,
            boolean expired
    ) {
        public static InventoryResponse fromEntity(BloodInventory item) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), item.getExpiryDate());
            boolean isExpired = days < 0 || item.getStatus() == InventoryStatus.EXPIRED;
            boolean isExpiringSoon = days >= 0 && days <= 7 && item.getStatus() == InventoryStatus.AVAILABLE;

            return new InventoryResponse(
                    item.getId(),
                    item.getBloodGroup(),
                    item.getRhType(),
                    item.getQuantity(),
                    item.getCollectionDate(),
                    item.getExpiryDate(),
                    item.getBatchUnitId(),
                    item.getFacilityName(),
                    item.getStatus(),
                    days,
                    isExpiringSoon,
                    isExpired
            );
        }
    }

    public record BloodGroupStockDto(
            BloodGroup bloodGroup,
            long totalQuantity,
            boolean isLowStock
    ) {}

    public record InventoryAlertsResponse(
            List<InventoryResponse> expiringBatches,
            List<BloodGroupStockDto> stockSummary,
            int totalAvailableUnits,
            int totalReservedUnits,
            int expiringUnitsCount
    ) {}

    public record UpdateInventoryQuantityRequest(
            @NotNull @Min(1) Integer quantity,
            String notes
    ) {}
}
