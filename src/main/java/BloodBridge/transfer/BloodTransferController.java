package BloodBridge.transfer;

import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.transfer.BloodTransferDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfers")
public class BloodTransferController {

    private final BloodTransferService transferService;
    private final UserAccountRepository userAccountRepository;

    public BloodTransferController(
            BloodTransferService transferService,
            UserAccountRepository userAccountRepository) {
        this.transferService = transferService;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            Authentication authentication,
            @Valid @RequestBody CreateTransferRequest request) {
        UserAccount user = null;
        if (authentication != null && authentication.isAuthenticated()) {
            user = userAccountRepository.findByEmail(authentication.getName()).orElse(null);
        }
        TransferResponse response = transferService.requestTransfer(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransferResponse>> listTransfers(
            @RequestParam(required = false) String hospital,
            @RequestParam(required = false) TransferStatus status) {
        return ResponseEntity.ok(transferService.listTransfers(hospital, status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TransferResponse> updateStatus(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTransferStatusRequest request) {
        UserAccount actor = null;
        if (authentication != null && authentication.isAuthenticated()) {
            actor = userAccountRepository.findByEmail(authentication.getName()).orElse(null);
        }
        TransferResponse response = transferService.updateStatus(id, request.status(), request.trackingNotes(), actor);
        return ResponseEntity.ok(response);
    }
}
