package BloodBridge.request;

import BloodBridge.common.BloodGroup;
import BloodBridge.request.BloodRequestDtos.BloodRequestResponse;
import BloodBridge.request.BloodRequestDtos.CreateBloodRequest;
import BloodBridge.request.BloodRequestDtos.DonorInterestResponse;
import BloodBridge.request.BloodRequestDtos.MyDonorInterestResponse;
import BloodBridge.request.BloodRequestDtos.UpdateInterestStatus;
import BloodBridge.request.BloodRequestDtos.UpdateRequestStatus;
import BloodBridge.request.BloodRequestDtos.TimelineEventResponse;
import BloodBridge.request.BloodRequestDtos.UpdateFulfillmentRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blood-requests")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;

    public BloodRequestController(BloodRequestService bloodRequestService) {
        this.bloodRequestService = bloodRequestService;
    }

    @PostMapping
    public ResponseEntity<BloodRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateBloodRequest payload) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bloodRequestService.create(authentication.getName(), payload));
    }

    @GetMapping
    public List<BloodRequestResponse> listOpen(
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(required = false) String city) {
        return bloodRequestService.listOpen(bloodGroup, city);
    }

    @GetMapping("/emergencies")
    public List<BloodRequestResponse> listEmergencies(
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(required = false) String city) {
        return bloodRequestService.listEmergencies(bloodGroup, city);
    }

    @GetMapping("/me")
    public List<BloodRequestResponse> listMine(Authentication authentication) {
        return bloodRequestService.listMine(authentication.getName());
    }

    @GetMapping("/interests/me")
    public List<MyDonorInterestResponse> listMyInterests(Authentication authentication) {
        return bloodRequestService.listMyInterests(authentication.getName());
    }

    @PostMapping("/{requestId}/interests")
    public ResponseEntity<Void> registerInterest(Authentication authentication, @PathVariable long requestId) {
        bloodRequestService.registerInterest(authentication.getName(), requestId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{requestId}/interests")
    public List<DonorInterestResponse> listInterests(Authentication authentication, @PathVariable long requestId) {
        return bloodRequestService.listInterests(authentication.getName(), requestId);
    }

    @GetMapping("/{requestId}/timeline")
    public List<TimelineEventResponse> getTimeline(@PathVariable long requestId) {
        return bloodRequestService.getTimeline(requestId);
    }

    @PatchMapping("/{requestId}/fulfillment")
    public BloodRequestResponse updateFulfillment(
            Authentication authentication,
            @PathVariable long requestId,
            @Valid @RequestBody UpdateFulfillmentRequest payload) {
        return bloodRequestService.updateFulfillment(authentication.getName(), requestId, payload.unitsFulfilled());
    }

    @PatchMapping("/{requestId}/status")
    public BloodRequestResponse updateStatus(
            Authentication authentication,
            @PathVariable long requestId,
            @Valid @RequestBody UpdateRequestStatus payload) {
        return bloodRequestService.updateStatus(authentication.getName(), requestId, payload);
    }

    @PatchMapping("/{requestId}/interests/{interestId}")
    public DonorInterestResponse updateInterestStatus(
            Authentication authentication,
            @PathVariable long requestId,
            @PathVariable long interestId,
            @Valid @RequestBody UpdateInterestStatus payload) {
        return bloodRequestService.updateInterestStatus(authentication.getName(), requestId, interestId, payload);
    }

    @GetMapping("/flagged")
    public List<BloodRequestResponse> listFlaggedRequests() {
        return bloodRequestService.getFlaggedRequests();
    }

    @PatchMapping("/{requestId}/risk-review")
    public BloodRequestResponse reviewRisk(
            Authentication authentication,
            @PathVariable long requestId,
            @Valid @RequestBody BloodRequestDtos.RiskReviewRequest payload) {
        return bloodRequestService.reviewRisk(requestId, payload, authentication.getName());
    }
}
