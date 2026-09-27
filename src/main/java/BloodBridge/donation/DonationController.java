package BloodBridge.donation;

import BloodBridge.donation.DonationDtos.CreateDonation;
import BloodBridge.donation.DonationDtos.DonationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping
    public ResponseEntity<DonationResponse> record(
            Authentication authentication,
            @Valid @RequestBody CreateDonation payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donationService.record(authentication.getName(), payload));
    }

    @GetMapping("/me")
    public List<DonationResponse> listMine(Authentication authentication) {
        return donationService.listMine(authentication.getName());
    }

    @GetMapping("/certificates")
    public List<DonationDtos.DonationCertificateDto> listCertificates(Authentication authentication) {
        return donationService.getCertificates(authentication.getName());
    }
}
