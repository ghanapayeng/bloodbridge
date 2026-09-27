package BloodBridge.escalation;

import BloodBridge.escalation.EscalationDtos.EscalationLogResponse;
import BloodBridge.escalation.EscalationDtos.EscalationResultResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-requests/{id}")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping("/escalate")
    public ResponseEntity<EscalationResultResponse> escalateRequest(@PathVariable("id") Long id) {
        EscalationResultResponse response = escalationService.escalateRequest(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/escalation-logs")
    public ResponseEntity<List<EscalationLogResponse>> getEscalationLogs(@PathVariable("id") Long id) {
        List<EscalationLogResponse> logs = escalationService.getEscalationHistory(id);
        return ResponseEntity.ok(logs);
    }
}
