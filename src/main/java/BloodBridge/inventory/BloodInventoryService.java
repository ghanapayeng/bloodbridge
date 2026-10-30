package BloodBridge.inventory;

import BloodBridge.audit.AuditLogService;
import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.common.BloodComponent;
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

    @Value("${bloodbridge.inventory.critical-stock-threshold:3}")
    private int criticalStockThreshold;

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

        BloodComponent component = request.component() != null ? request.component() : BloodComponent.WHOLE_BLOOD;
        boolean isDemo = Boolean.TRUE.equals(request.isDemo());

        BloodInventory item = new BloodInventory(
                request.bloodGroup(),
                rhType,
                component,
                request.quantity(),
                request.collectionDate(),
                request.expiryDate(),
                batchId,
                request.facilityName(),
                InventoryStatus.AVAILABLE,
                isDemo
        );

        BloodInventory saved = inventoryRepository.save(item);

        auditLogService.logAction(
                createdByEmail,
                "INVENTORY_ADDED",
                "BloodInventory",
                saved.getId(),
                "SUCCESS",
                "Added " + saved.getQuantity() + " units of " + saved.getBloodGroup() + " (" + component.getDisplayName() + ") to " + saved.getFacilityName()
        );

        return InventoryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventory(String facilityName, InventoryStatus status, BloodGroup bloodGroup) {
        return getInventory(facilityName, status, bloodGroup, null);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventory(String facilityName, InventoryStatus status, BloodGroup bloodGroup, BloodComponent component) {
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

        if (component != null) {
            list = list.stream().filter(i -> i.getComponent() == component).toList();
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
            item.setQuantity(item.getQuantity() - quantityToReserve);
            inventoryRepository.save(item);

            BloodInventory reservedBatch = new BloodInventory(
                    item.getBloodGroup(),
                    item.getRhType(),
                    item.getComponent(),
                    quantityToReserve,
                    item.getCollectionDate(),
                    item.getExpiryDate(),
                    item.getBatchUnitId() + "-RES",
                    item.getFacilityName(),
                    InventoryStatus.RESERVED,
                    item.isDemo()
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
                    item.getComponent(),
                    quantityToIssue,
                    item.getCollectionDate(),
                    item.getExpiryDate(),
                    item.getBatchUnitId() + "-TRF",
                    item.getFacilityName(),
                    InventoryStatus.ISSUED,
                    item.isDemo()
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

        List<BloodInventory> allAvailable = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);

        Map<BloodGroup, Long> stockMap = new EnumMap<>(BloodGroup.class);
        Map<BloodGroup, Map<String, Integer>> breakdownMap = new EnumMap<>(BloodGroup.class);
        for (BloodGroup bg : BloodGroup.values()) {
            stockMap.put(bg, 0L);
            breakdownMap.put(bg, new LinkedHashMap<>());
        }

        for (BloodInventory item : allAvailable) {
            BloodGroup bg = item.getBloodGroup();
            stockMap.put(bg, stockMap.getOrDefault(bg, 0L) + item.getQuantity());

            String compCode = item.getComponent() != null ? item.getComponent().getCode() : "WB";
            Map<String, Integer> compMap = breakdownMap.get(bg);
            compMap.put(compCode, compMap.getOrDefault(compCode, 0) + item.getQuantity());
        }

        List<BloodGroupStockDto> stockSummary = new ArrayList<>();
        int totalAvailable = 0;
        for (Map.Entry<BloodGroup, Long> entry : stockMap.entrySet()) {
            BloodGroup bg = entry.getKey();
            long qty = entry.getValue();
            boolean isLow = qty < lowStockThreshold;
            boolean isCritical = qty < criticalStockThreshold;
            stockSummary.add(new BloodGroupStockDto(
                    bg,
                    bg.getDisplay(),
                    qty,
                    isLow,
                    isCritical,
                    breakdownMap.get(bg)
            ));
            totalAvailable += qty;
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

    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary() {
        InventoryAlertsResponse alerts = getInventoryAlerts();

        List<BloodInventory> allIssued = inventoryRepository.findByStatus(InventoryStatus.ISSUED);
        int totalIssued = allIssued.stream().mapToInt(BloodInventory::getQuantity).sum();

        Map<BloodComponent, Long> componentMap = new EnumMap<>(BloodComponent.class);
        for (BloodComponent c : BloodComponent.values()) {
            componentMap.put(c, 0L);
        }

        List<BloodInventory> available = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE);
        for (BloodInventory item : available) {
            BloodComponent c = item.getComponent() != null ? item.getComponent() : BloodComponent.WHOLE_BLOOD;
            componentMap.put(c, componentMap.getOrDefault(c, 0L) + item.getQuantity());
        }

        List<ComponentStockDto> componentList = new ArrayList<>();
        for (Map.Entry<BloodComponent, Long> entry : componentMap.entrySet()) {
            BloodComponent comp = entry.getKey();
            componentList.add(new ComponentStockDto(
                    comp,
                    comp.getDisplayName(),
                    comp.getCode(),
                    entry.getValue(),
                    comp.getShelfLifeDays()
            ));
        }

        int lowStockCount = (int) alerts.stockSummary().stream().filter(BloodGroupStockDto::isLowStock).count();
        int criticalCount = (int) alerts.stockSummary().stream().filter(BloodGroupStockDto::isCriticalShortage).count();
        boolean hasDemoData = inventoryRepository.countByIsDemoTrue() > 0;

        return new InventorySummaryResponse(
                alerts.totalAvailableUnits(),
                alerts.totalReservedUnits(),
                totalIssued,
                alerts.expiringUnitsCount(),
                lowStockCount,
                criticalCount,
                alerts.stockSummary(),
                componentList,
                hasDemoData,
                hasDemoData ? "ESTIMATED DEMONSTRATION INVENTORY (ACADEMIC USE ONLY)" : "REAL OPERATIONAL INVENTORY"
        );
    }

    /**
     * Daily background job to mark expired units and alert staff for genuine units expiring in <= 3 days.
     * Demonstration units are safely excluded from generating real staff alerts.
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

        // 2. Alert hospital coordinators for genuine physical units expiring soon (<= 3 days)
        LocalDate threeDaysOut = today.plusDays(3);
        List<BloodInventory> criticalExpiry = inventoryRepository.findApproachingOrExpired(InventoryStatus.AVAILABLE, threeDaysOut)
                .stream()
                .filter(b -> !b.isDemo())
                .toList();

        if (!criticalExpiry.isEmpty()) {
            List<UserAccount> hospitals = userAccountRepository.findByRole("ROLE_HOSPITAL");
            for (UserAccount hospital : hospitals) {
                appNotificationRepository.save(new AppNotification(
                        hospital,
                        NotificationType.EXPIRY_WARNING,
                        "Blood Units Expiring Soon Alert",
                        criticalExpiry.size() + " genuine blood batches are expiring within 3 days. Please prioritize utilization or transfer.",
                        null
                ));
            }
        }
    }
}
