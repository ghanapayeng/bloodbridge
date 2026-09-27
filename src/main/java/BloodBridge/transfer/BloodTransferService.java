package BloodBridge.transfer;

import BloodBridge.audit.AuditLogService;
import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.inventory.BloodInventory;
import BloodBridge.inventory.BloodInventoryRepository;
import BloodBridge.inventory.InventoryStatus;
import BloodBridge.notification.AppNotification;
import BloodBridge.notification.AppNotificationRepository;
import BloodBridge.notification.NotificationType;
import BloodBridge.transfer.BloodTransferDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BloodTransferService {

    private final BloodTransferRepository transferRepository;
    private final BloodInventoryRepository inventoryRepository;
    private final AuditLogService auditLogService;
    private final AppNotificationRepository appNotificationRepository;
    private final UserAccountRepository userAccountRepository;

    public BloodTransferService(
            BloodTransferRepository transferRepository,
            BloodInventoryRepository inventoryRepository,
            AuditLogService auditLogService,
            AppNotificationRepository appNotificationRepository,
            UserAccountRepository userAccountRepository) {
        this.transferRepository = transferRepository;
        this.inventoryRepository = inventoryRepository;
        this.auditLogService = auditLogService;
        this.appNotificationRepository = appNotificationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public TransferResponse requestTransfer(CreateTransferRequest request, UserAccount requestedBy) {
        if (request.sourceHospital().trim().equalsIgnoreCase(request.destinationHospital().trim())) {
            throw new IllegalArgumentException("Source and destination hospital cannot be identical.");
        }

        BloodTransfer transfer = new BloodTransfer(
                request.sourceHospital().trim(),
                request.destinationHospital().trim(),
                request.bloodGroup(),
                request.units(),
                request.notes(),
                requestedBy
        );

        BloodTransfer saved = transferRepository.save(transfer);

        // Notify hospitals
        List<UserAccount> hospitals = userAccountRepository.findByRole("ROLE_HOSPITAL");
        for (UserAccount h : hospitals) {
            appNotificationRepository.save(new AppNotification(
                    h,
                    NotificationType.TRANSFER_REQUEST,
                    "New Blood Transfer Request: " + saved.getBloodGroup(),
                    saved.getDestinationHospital() + " has requested " + saved.getUnits()
                            + " units of " + saved.getBloodGroup() + " from " + saved.getSourceHospital() + ".",
                    null
            ));
        }

        auditLogService.logAction(
                requestedBy != null ? requestedBy.getEmail() : "system",
                "TRANSFER_REQUESTED",
                "BloodTransfer",
                saved.getId(),
                "SUCCESS",
                "Requested transfer of " + saved.getUnits() + " units from " + saved.getSourceHospital() + " to " + saved.getDestinationHospital()
        );

        return TransferResponse.fromEntity(saved);
    }

    @Transactional
    public TransferResponse updateStatus(Long transferId, TransferStatus newStatus, String trackingNotes, UserAccount actor) {
        BloodTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found with ID: " + transferId));

        TransferStatus oldStatus = transfer.getStatus();

        // 1. Stock reservation on approval
        if (newStatus == TransferStatus.APPROVED && oldStatus == TransferStatus.REQUESTED) {
            List<BloodInventory> sourceStock = inventoryRepository.findByFacilityNameIgnoreCaseAndStatus(
                    transfer.getSourceHospital(), InventoryStatus.AVAILABLE
            ).stream().filter(i -> i.getBloodGroup() == transfer.getBloodGroup()).toList();

            int availableUnits = sourceStock.stream().mapToInt(BloodInventory::getQuantity).sum();
            if (availableUnits < transfer.getUnits()) {
                throw new IllegalStateException("Source hospital has only " + availableUnits + " available units of "
                        + transfer.getBloodGroup() + ". Required: " + transfer.getUnits());
            }

            // Reserve units from available batches
            int remainingToReserve = transfer.getUnits();
            for (BloodInventory batch : sourceStock) {
                if (remainingToReserve <= 0) break;
                int take = Math.min(batch.getQuantity(), remainingToReserve);
                if (take == batch.getQuantity()) {
                    batch.setStatus(InventoryStatus.RESERVED);
                    inventoryRepository.save(batch);
                } else {
                    batch.setQuantity(batch.getQuantity() - take);
                    inventoryRepository.save(batch);

                    BloodInventory reservedSub = new BloodInventory(
                            batch.getBloodGroup(),
                            batch.getRhType(),
                            take,
                            batch.getCollectionDate(),
                            batch.getExpiryDate(),
                            batch.getBatchUnitId() + "-TXRES",
                            batch.getFacilityName(),
                            InventoryStatus.RESERVED
                    );
                    inventoryRepository.save(reservedSub);
                }
                remainingToReserve -= take;
            }
        }

        // 2. Receipt and stock deposit at destination on received
        if (newStatus == TransferStatus.RECEIVED && oldStatus == TransferStatus.DISPATCHED) {
            LocalDate today = LocalDate.now();
            String rhType = transfer.getBloodGroup().name().contains("POSITIVE") ? "+" : "-";

            // Add received units into destination inventory
            BloodInventory receivedBatch = new BloodInventory(
                    transfer.getBloodGroup(),
                    rhType,
                    transfer.getUnits(),
                    today,
                    today.plusDays(35),
                    "TRF-REC-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(),
                    transfer.getDestinationHospital(),
                    InventoryStatus.AVAILABLE
            );
            inventoryRepository.save(receivedBatch);
        }

        transfer.setStatus(newStatus);
        if (trackingNotes != null && !trackingNotes.isBlank()) {
            String updatedNotes = (transfer.getNotes() != null ? transfer.getNotes() + "\n" : "")
                    + "[" + newStatus + "] " + trackingNotes.trim();
            transfer.setNotes(updatedNotes);
        }

        BloodTransfer updated = transferRepository.save(transfer);

        // Notify requester if available
        if (transfer.getRequestedBy() != null) {
            appNotificationRepository.save(new AppNotification(
                    transfer.getRequestedBy(),
                    NotificationType.TRANSFER_REQUEST,
                    "Transfer " + newStatus + ": " + transfer.getBloodGroup(),
                    "Transfer of " + transfer.getUnits() + " units from " + transfer.getSourceHospital()
                            + " is now " + newStatus + ".",
                    null
            ));
        }

        auditLogService.logAction(
                actor != null ? actor.getEmail() : "system",
                "TRANSFER_STATUS_" + newStatus,
                "BloodTransfer",
                updated.getId(),
                "SUCCESS",
                "Transfer status changed from " + oldStatus + " to " + newStatus
        );

        return TransferResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> listTransfers(String hospitalName, TransferStatus status) {
        List<BloodTransfer> list;
        if (hospitalName != null && !hospitalName.isBlank()) {
            list = transferRepository.findBySourceHospitalIgnoreCaseOrDestinationHospitalIgnoreCaseOrderByCreatedAtDesc(
                    hospitalName, hospitalName
            );
        } else if (status != null) {
            list = transferRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = transferRepository.findAllByOrderByCreatedAtDesc();
        }

        if (status != null && hospitalName != null && !hospitalName.isBlank()) {
            list = list.stream().filter(t -> t.getStatus() == status).toList();
        }

        return list.stream().map(TransferResponse::fromEntity).toList();
    }
}
