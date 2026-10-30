package BloodBridge.inventory;

import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class BloodInventoryDtos {

    public record CreateInventoryRequest(
            @NotNull BloodGroup bloodGroup,
            String rhType,
            BloodComponent component,
            @NotNull @Min(1) Integer quantity,
            @NotNull LocalDate collectionDate,
            @NotNull LocalDate expiryDate,
            String batchUnitId,
            @NotBlank String facilityName,
            Boolean isDemo
    ) {}

    public record InventoryResponse(
            Long id,
            BloodGroup bloodGroup,
            String rhType,
            BloodComponent component,
            int quantity,
            LocalDate collectionDate,
            LocalDate expiryDate,
            String batchUnitId,
            String facilityName,
            InventoryStatus status,
            boolean isDemo,
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
                    item.getComponent(),
                    item.getQuantity(),
                    item.getCollectionDate(),
                    item.getExpiryDate(),
                    item.getBatchUnitId(),
                    item.getFacilityName(),
                    item.getStatus(),
                    item.isDemo(),
                    days,
                    isExpiringSoon,
                    isExpired
            );
        }
    }

    public record BloodGroupStockDto(
            BloodGroup bloodGroup,
            String bloodGroupDisplay,
            long totalQuantity,
            boolean isLowStock,
            boolean isCriticalShortage,
            Map<String, Integer> componentBreakdown
    ) {}

    public record ComponentStockDto(
            BloodComponent component,
            String componentName,
            String componentCode,
            long totalQuantity,
            int shelfLifeDays
    ) {}

    public record InventorySummaryResponse(
            int totalAvailableUnits,
            int totalReservedUnits,
            int totalIssuedUnits,
            int expiringUnitsCount,
            int lowStockCount,
            int criticalShortageCount,
            List<BloodGroupStockDto> bloodGroups,
            List<ComponentStockDto> components,
            boolean hasDemoData,
            String notice
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
