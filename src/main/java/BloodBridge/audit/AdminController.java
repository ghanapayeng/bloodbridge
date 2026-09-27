package BloodBridge.audit;

import BloodBridge.auth.UserAccountRepository;
import BloodBridge.donation.DonationRepository;
import BloodBridge.donor.DonorProfileRepository;
import BloodBridge.inventory.BloodInventory;
import BloodBridge.inventory.BloodInventoryRepository;
import BloodBridge.inventory.InventoryStatus;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestRepository;
import BloodBridge.request.BloodRequestStatus;
import BloodBridge.request.RequestRiskLevel;
import BloodBridge.transfer.BloodTransfer;
import BloodBridge.transfer.BloodTransferRepository;
import BloodBridge.transfer.TransferStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuditLogService auditLogService;
    private final UserAccountRepository userRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonationRepository donationRepository;
    private final BloodInventoryRepository inventoryRepository;
    private final BloodTransferRepository transferRepository;

    public AdminController(
            AuditLogService auditLogService,
            UserAccountRepository userRepository,
            DonorProfileRepository donorProfileRepository,
            BloodRequestRepository bloodRequestRepository,
            DonationRepository donationRepository,
            BloodInventoryRepository inventoryRepository,
            BloodTransferRepository transferRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donationRepository = donationRepository;
        this.inventoryRepository = inventoryRepository;
        this.transferRepository = transferRepository;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditLogService.getRecentLogs(limit));
    }

    @GetMapping("/telemetry")
    public ResponseEntity<AdminTelemetryDto> getTelemetry() {
        long users = userRepository.count();
        long donors = donorProfileRepository.count();
        List<BloodRequest> allRequests = bloodRequestRepository.findAll();
        long openReqs = allRequests.stream().filter(r -> r.getStatus() == BloodRequestStatus.OPEN).count();
        long emergencies = allRequests.stream()
                .filter(r -> r.getStatus() == BloodRequestStatus.OPEN && r.getUrgency() != null && r.getUrgency().isEmergency())
                .count();
        long flagged = allRequests.stream()
                .filter(r -> r.getRiskLevel() != null && r.getRiskLevel() != RequestRiskLevel.NORMAL)
                .count();
        long donations = donationRepository.count();
        int inventoryUnits = inventoryRepository.findByStatus(InventoryStatus.AVAILABLE).stream()
                .mapToInt(BloodInventory::getQuantity)
                .sum();
        long activeTransfers = transferRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransferStatus.REQUESTED
                        || t.getStatus() == TransferStatus.APPROVED
                        || t.getStatus() == TransferStatus.DISPATCHED)
                .count();

        AdminTelemetryDto telemetry = new AdminTelemetryDto(
                users,
                donors,
                allRequests.size(),
                openReqs,
                emergencies,
                flagged,
                donations,
                inventoryUnits,
                activeTransfers,
                "HEALTHY",
                Instant.now()
        );
        return ResponseEntity.ok(telemetry);
    }
}
