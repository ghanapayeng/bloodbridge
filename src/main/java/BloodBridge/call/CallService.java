package BloodBridge.call;

import BloodBridge.auth.AuthService;
import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.call.CallDtos.CallSignalResponse;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CallService {

    private final CallSignalRepository callSignalRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthService authService;

    public CallService(
            CallSignalRepository callSignalRepository,
            BloodRequestRepository bloodRequestRepository,
            UserAccountRepository userAccountRepository,
            AuthService authService) {
        this.callSignalRepository = callSignalRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.authService = authService;
    }

    @Transactional
    public CallSignalResponse sendSignal(String currentUserEmail, Long requestId, Long targetUserId, String signalType, String signalData) {
        UserAccount sender = authService.requireByEmail(currentUserEmail);
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found."));
        UserAccount targetUser = userAccountRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found."));

        CallSignal signal = new CallSignal(request, sender, targetUser, signalType, signalData);
        CallSignal saved = callSignalRepository.save(signal);
        return CallSignalResponse.from(saved);
    }

    @Transactional
    public List<CallSignalResponse> pollSignals(String currentUserEmail) {
        UserAccount user = authService.requireByEmail(currentUserEmail);
        List<CallSignal> signals = callSignalRepository.findPendingSignalsForRecipient(user.getId());
        if (!signals.isEmpty()) {
            callSignalRepository.markConsumedForRecipient(user.getId());
        }
        return signals.stream().map(CallSignalResponse::from).toList();
    }
}
