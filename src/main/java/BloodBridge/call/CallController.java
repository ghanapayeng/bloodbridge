package BloodBridge.call;

import BloodBridge.call.CallDtos.CallSignalResponse;
import BloodBridge.call.CallDtos.SendSignalRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    @PostMapping("/signal")
    public ResponseEntity<CallSignalResponse> sendSignal(
            Authentication authentication,
            @Valid @RequestBody SendSignalRequest request) {
        CallSignalResponse response = callService.sendSignal(
                authentication.getName(),
                request.requestId(),
                request.getTargetUserId(),
                request.signalType(),
                request.signalData());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/poll")
    public List<CallSignalResponse> pollSignals(Authentication authentication) {
        return callService.pollSignals(authentication.getName());
    }
}
