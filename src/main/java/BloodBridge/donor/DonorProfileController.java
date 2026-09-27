package BloodBridge.donor;

import BloodBridge.common.BloodGroup;
import BloodBridge.donor.DonorDtos.DonorMatchResponse;
import BloodBridge.donor.DonorDtos.MyDonorProfileResponse;
import BloodBridge.donor.DonorDtos.UpsertDonorProfileRequest;
import BloodBridge.donor.DonorDtos.SmartDonorMatchDto;
import BloodBridge.request.RequestUrgency;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DonorProfileController {

    private final DonorProfileService donorProfileService;
    private final SmartDonorMatchingService smartDonorMatchingService;
    private final DonorEligibilityService donorEligibilityService;

    public DonorProfileController(
            DonorProfileService donorProfileService,
            SmartDonorMatchingService smartDonorMatchingService,
            DonorEligibilityService donorEligibilityService) {
        this.donorProfileService = donorProfileService;
        this.smartDonorMatchingService = smartDonorMatchingService;
        this.donorEligibilityService = donorEligibilityService;
    }

    @GetMapping("/api/donor-profiles/me")
    public MyDonorProfileResponse mine(Authentication authentication) {
        return donorProfileService.getMine(authentication.getName());
    }

    @GetMapping("/api/donor-profiles/me/eligibility")
    public ResponseEntity<DonorDtos.DonorEligibilityResponse> getMyEligibility(Authentication authentication) {
        Long userId = donorProfileService.getUserIdByEmail(authentication.getName());
        return ResponseEntity.ok(donorEligibilityService.evaluateEligibility(userId));
    }

    @PutMapping("/api/donor-profiles/me")
    public ResponseEntity<MyDonorProfileResponse> saveMine(
            Authentication authentication,
            @Valid @RequestBody UpsertDonorProfileRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(donorProfileService.saveMine(authentication.getName(), request));
    }

    @GetMapping("/api/donors")
    public List<DonorMatchResponse> findDonors(
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(required = false) String city) {
        return donorProfileService.findAvailable(bloodGroup, city);
    }

    @GetMapping("/api/donors/smart-match")
    public List<SmartDonorMatchDto> smartMatch(
            Authentication authentication,
            @RequestParam(required = false) BloodGroup bloodGroup,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) RequestUrgency urgency) {
        Long excludeUserId = null;
        if (authentication != null && authentication.isAuthenticated()) {
            excludeUserId = donorProfileService.getUserIdByEmail(authentication.getName());
        }
        return smartDonorMatchingService.rankDonors(bloodGroup, city, latitude, longitude, urgency, excludeUserId);
    }

    @GetMapping("/api/donors/verify/{token}")
    public ResponseEntity<DonorDtos.DonorVerificationResponse> verifyDonor(@PathVariable("token") String token) {
        return ResponseEntity.ok(donorProfileService.verifyDonor(token));
    }
}
