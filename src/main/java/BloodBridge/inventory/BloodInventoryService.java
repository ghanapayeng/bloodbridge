package BloodBridge.inventory;

import BloodBridge.audit.AuditLogService;
import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.common.BloodGroup;
import BloodBridge.inventory.BloodInventoryDtos.*;
import BloodBridge.notification.AppNotification;
import BloodBridge.notification.AppNotificationRepository;
import BloodBridge.notification.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class BloodInventoryService {

    private static final Logger log = LoggerFactory.getLogger(BloodInventoryService.class);

    private final BloodInventoryRepository inventoryRepository;
    private final AuditLogService auditLogService;
    private final AppNotificationRepository appNotificationRepository;
    private final UserAccountRepository userAccountRepository;

    @Value("${bloodbridge.inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    public BloodInventoryService(
            BloodInventoryRepository inventoryRepository,
            AuditLogService auditLogService,
            AppNotificationRepository appNotificationRepository,
            UserAccountRepository userAccountRepository) {
        this.inventoryRepository = inventoryRepository;
        this.auditLogService = auditLogService;
        this.appNotificationRepository = appNotificationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public InventoryResponse addInventoryBatch(CreateInventoryRequest request, String createdByEmail) {
        if (request.expiryDate().isBefore(request.collectionDate())) {
            throw new IllegalArgumentException("Expiry date cannot be before collection date.");
        }

        String batchId = request.batchUnitId();
        if (batchId == null || batchId.isBlank()) {
            batchId = "BATCH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        String rhType = request.rhType();
        if (rhType == null || rhType.isBlank()) {
            rhType = request.bloodGroup().name().contains("POSITIVE") ? "+" : "-";
        }

        BloodInventory item = new BloodInventory(
                request.bloodGroup(),
                rhType,
                request.quantity(),
                request.collectionDate(),
                request.expiryDate(),
                batchId,
                request.facilityName(),
                InventoryStatus.AVAILABLE
        );

        BloodInventory saved = inventoryRepository.save(item);

        auditLogService.logAction(
                createdByEmail,
                "INVENTORY_ADDED",
                "BloodInventory",
                saved.getId(),
                "SUCCESS",
                "Added " + saved.getQuantity() + " units of " + saved.getBloodGroup() + " to " + saved.getFacilityName()
        );

        return InventoryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventory(String facilityName, InventoryStatus status, BloodGroup bloodGroup) {
        List<BloodInventory> list;
        if (facilityName != null && !facilityName.isBlank()) {
            if (status != null) {
                list = inventoryRepository.findByFacilityNameIgnoreCaseAndStatus(facilityName, status);
            } else {
                list = inventoryRepository.findByFacilityNameIgnoreCase(facilityName);
            }
        } else if (status != null) {
            list = inventoryRepository.findByStatus(status);
        } else {
            list = inventoryRepository.findAll();
        }

        if (bloodGroup != null) {
            list = list.stream().filter(i -> i.getBloodGroup() == bloodGroup).toList();
        }

        return list.stream().map(InventoryResponse::fromEntity).toList();
    }

    @Transactional
    public InventoryResponse reserveUnits(Long inventoryId, int quantityToReserve, String requestedByEmail) {
        if (quantityToReserve <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive.");
        }

        BloodInventory item = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with ID: " + inventoryId));

        if (item.getStatus() != InventoryStatus.AVAILABLE || item.getQuantity() < quantityToReserve) {
            throw new IllegalStateException("Insufficient available units. Available: "
                    + (item.getStatus() == InventoryStatus.AVAILABLE ? item.getQuantity() : 0));
        }

        if (item.getQuantity() == quantityToReserve) {
            item.setStatus(InventoryStatus.RESERVED);
            inventoryRepository.save(item);
        } else {
            // Split batch
            item.setQuantity(item.getQuantity() - quantityToReserve);
            inventoryRepository.save(item);

            BloodInventory reservedBatch = new BloodInventory(
                    item.getBloodGroup(),
                    item.getRhType(),
                    quantityToReserve,
                    item.getCollectionDate(),
                    item.getExpiryDate(),
                    item.getBatchUnitId() + "-RES",
                    item.getFacilityName(),
                    InventoryStatus.RESERVED
            );
            inventoryRepository.save(reservedBatch);
        }

        auditLogService.logAction(
                requestedByEmail,
                "INVENTORY_RESERVED",
                "BloodInventory",
                item.getId(),
                "SUCCESS",
                "Reserved " + quantityToReserve + " units of " + item.getBloodGroup()
        );

        return InventoryResponse.fromEntity(item);
    }

    @Transactional
    public InventoryResponse issueUnits(Long inventoryId, int quantityToIssue, String issuedByEmail) {
        if (quantityToIssue <= 0) {
            throw new IllegalArgumentException("Issued quantity must be positive.");
        }

        BloodInventory item = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with ID: " + inventoryId));

        if (item.getQuantity() < quantityToIssue) {
            throw new IllegalStateException("Cannot issue more units than batch quantity (" + item.getQuantity() + ").");
        }

        if (item.getQuantity() == quantityToIssue) {
            item.setStatus(InventoryStatus.ISSUED);
            inventoryRepository.save(item);
        } else {
            item.setQuantity(item.getQuantity() - quantityToIssue);
            inventoryRepository.save(item);

            BloodInventory transfusedBatch = new BloodInventory(
                    item.getBloodGroup(),
                    item.getRhType(),
                    quantityToIssue,
                    item.getCollectionDate(),
                    item.getExpiryDate(),
                    item.getBatchUnitId() + "-TRF",
                    item.getFacilityName(),
                    InventoryStatus.ISSUED
            );
            inventoryRepository.save(transfusedBatch);
        }

        auditLogService.logAction(
                issuedByEmail,
                "INVENTORY_ISSUED",
                "BloodInventory",
                item.getId(),
                "SUCCESS",
                "Issued/transfused " + quantityToIssue + " units of " + item.getBloodGroup()
        );

        return InventoryResponse.fromEntity(item);
    }

    @Transactional(readOnly = true)
    public InventoryAlertsResponse getInventoryAlerts() {
        LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
        List<BloodInventory> expiringBatches = inventoryRepository.findApproachingOrExpired(
                InventoryStatus.AVAILABLE, sevenDaysFromNow
        );

        Map<BloodGroup, Long> stockMap = new EnumMap<>(BloodGroup.class);
        for (BloodGroup bg : BloodGroup.values()) {
            stockMap.put(bg, 0L);
        }

        List<Object[]> rows = inventoryRepository.sumAvailableQuantityByBloodGroup();
        for (Object[] row : rows) {
            BloodGroup bg = (BloodGroup) row[0];
            Long total = ((Number) row[1]).longValue();
            stockMap.put(bg, total);
        }

        List<BloodGroupStockDto> stockSummary = new ArrayList<>();
        int totalAvailable = 0;
        for (Map.Entry<BloodGroup, Long> entry : stockMap.entrySet()) {
            boolean isLow = entry.getValue() < lowStockThreshold;
            stockSummary.add(new BloodGroupStockDto(entry.getKey(), entry.getValue(), isLow));
            totalAvailable += entry.getValue();
        }

        List<BloodInventory> reservedItems = inventoryRepository.findByStatus(InventoryStatus.RESERVED);
        int totalReserved = reservedItems.stream().mapToInt(BloodInventory::getQuantity).sum();

        int expiringUnitsCount = expiringBatches.stream().mapToInt(BloodInventory::getQuantity).sum();

        return new InventoryAlertsResponse(
                expiringBatches.stream().map(InventoryResponse::fromEntity).toList(),
                stockSummary,
                totalAvailable,
                totalReserved,
                expiringUnitsCount
        );
    }

    /**
     * Daily background job to mark expired units and alert staff for units expiring in <= 3 days.
     */
    @Scheduled(cron = "${bloodbridge.inventory.expiry-check-cron:0 0 1 * * *}")
    @Transactional
    public void checkAndAlertExpiry() {
        LocalDate today = LocalDate.now();

        // 1. Mark expired
        List<BloodInventory> available = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);
        for (BloodInventory item : available) {
            if (item.getExpiryDate().isBefore(today)) {
                item.setStatus(InventoryStatus.EXPIRED);
                inventoryRepository.save(item);
                log.info("Batch {} of {} marked EXPIRED at {}", item.getBatchUnitId(), item.getBloodGroup(), item.getFacilityName());
            }
        }

        // 2. Alert hospital coordinators for units expiring soon (<= 3 days)
        LocalDate threeDaysOut = today.plusDays(3);
        List<BloodInventory> criticalExpiry = inventoryRepository.findApproachingOrExpired(InventoryStatus.AVAILABLE, threeDaysOut);
        if (!criticalExpiry.isEmpty()) {
            List<UserAccount> hospitals = userAccountRepository.findByRole("ROLE_HOSPITAL");
            for (UserAccount hospital : hospitals) {
                appNotificationRepository.save(new AppNotification(
                        hospital,
                        NotificationType.EXPIRY_WARNING,
                        "Blood Units Expiring Soon Alert",
                        criticalExpiry.size() + " blood batches are expiring within 3 days. Please prioritize utilization or transfer.",
                        null
                ));
            }
        }
    }
}
