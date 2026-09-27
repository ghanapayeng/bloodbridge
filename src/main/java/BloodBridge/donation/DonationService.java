package BloodBridge.donation;

import BloodBridge.auth.AuthService;
import BloodBridge.auth.UserAccount;
import BloodBridge.donation.DonationDtos.CreateDonation;
import BloodBridge.donation.DonationDtos.DonationResponse;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestInterestRepository;
import BloodBridge.request.BloodRequestRepository;
import BloodBridge.common.BloodGroup;
import BloodBridge.donor.DonorProfile;
import BloodBridge.donor.DonorProfileRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DonationService {

    private final DonationRepository donations;
    private final BloodRequestRepository bloodRequests;
    private final BloodRequestInterestRepository interests;
    private final AuthService authService;
    private final DonorProfileRepository donorProfiles;

    public DonationService(
            DonationRepository donations,
            BloodRequestRepository bloodRequests,
            BloodRequestInterestRepository interests,
            AuthService authService,
            DonorProfileRepository donorProfiles) {
        this.donations = donations;
        this.bloodRequests = bloodRequests;
        this.interests = interests;
        this.authService = authService;
        this.donorProfiles = donorProfiles;
    }

    @Transactional
    public DonationResponse record(String email, CreateDonation payload) {
        UserAccount donor = authService.requireByEmail(email);
        BloodRequest bloodRequest = null;
        if (payload.bloodRequestId() != null) {
            bloodRequest = bloodRequests.findById(payload.bloodRequestId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found."));
            if (!interests.existsByBloodRequest_IdAndDonor_Id(bloodRequest.getId(), donor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only link a donation to a request you responded to.");
            }
        }
        Donation saved = donations.save(new Donation(
                donor, bloodRequest, payload.donatedAt(), payload.units()));

        donorProfiles.findByUser(donor).ifPresent(profile -> {
            if (profile.getLastDonationDate() == null || payload.donatedAt().isAfter(profile.getLastDonationDate())) {
                profile.setLastDonationDate(payload.donatedAt());
                donorProfiles.save(profile);
            }
        });

        return DonationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> listMine(String email) {
        return donations.findByDonorOrderByDonatedAtDesc(authService.requireByEmail(email)).stream()
                .map(DonationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DonationDtos.DonationCertificateDto> getCertificates(String email) {
        UserAccount user = authService.requireByEmail(email);
        DonorProfile profile = donorProfiles.findByUser(user).orElse(null);
        BloodGroup bg = profile != null ? profile.getBloodGroup() : null;

        return donations.findByDonorOrderByDonatedAtDesc(user).stream()
                .map(d -> {
                    String certNum = "CERT-BB-" + d.getDonatedAt().getYear() + "-" + String.format("%05d", d.getId());
                    String hospital = d.getBloodRequest() != null ? d.getBloodRequest().getHospital() : "BloodBridge Affiliated Center";
                    String city = d.getBloodRequest() != null ? d.getBloodRequest().getCity() : (profile != null ? profile.getCity() : "National");
                    BloodGroup bloodGroup = d.getBloodRequest() != null ? d.getBloodRequest().getBloodGroup() : bg;
                    String qrUrl = "/verify-donor.html?token=" + (user.getDonorUuid() != null ? user.getDonorUuid() : "");
                    return new DonationDtos.DonationCertificateDto(
                            d.getId(),
                            certNum,
                            user.getFullName(),
                            bloodGroup,
                            d.getUnits(),
                            d.getDonatedAt(),
                            hospital,
                            city,
                            qrUrl
                    );
                })
                .toList();
    }
}
