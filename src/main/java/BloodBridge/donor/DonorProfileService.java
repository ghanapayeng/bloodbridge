package BloodBridge.donor;

import BloodBridge.auth.AuthService;
import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodCompatibility;
import BloodBridge.common.BloodGroup;
import BloodBridge.donor.DonorDtos.DonorMatchResponse;
import BloodBridge.donor.DonorDtos.MyDonorProfileResponse;
import BloodBridge.donor.DonorDtos.UpsertDonorProfileRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DonorProfileService {

    private final DonorProfileRepository donorProfiles;
    private final AuthService authService;
    private final BloodBridge.auth.UserAccountRepository userAccounts;

    public DonorProfileService(
            DonorProfileRepository donorProfiles,
            AuthService authService,
            BloodBridge.auth.UserAccountRepository userAccounts) {
        this.donorProfiles = donorProfiles;
        this.authService = authService;
        this.userAccounts = userAccounts;
    }

    @Transactional(readOnly = true)
    public DonorDtos.DonorVerificationResponse verifyDonor(String token) {
        if (token == null || token.isBlank()) {
            return new DonorDtos.DonorVerificationResponse(false, null, null, null, 0, null, null, "Invalid verification token.");
        }
        var userOpt = userAccounts.findByDonorUuid(token.trim());
        if (userOpt.isEmpty()) {
            return new DonorDtos.DonorVerificationResponse(false, null, null, null, 0, null, null, "Donor card not recognized or invalid token.");
        }
        UserAccount user = userOpt.get();
        var profileOpt = donorProfiles.findByUser(user);
        if (profileOpt.isEmpty()) {
            return new DonorDtos.DonorVerificationResponse(false, null, null, null, 0, null, null, "Donor profile setup is incomplete.");
        }
        DonorProfile profile = profileOpt.get();

        String[] nameParts = user.getFullName().trim().split("\\s+");
        String maskedName = nameParts[0] + (nameParts.length > 1 ? " " + nameParts[nameParts.length - 1].charAt(0) + "." : "");

        int memberYear = user.getCreatedAt() != null
                ? java.time.LocalDateTime.ofInstant(user.getCreatedAt(), java.time.ZoneId.systemDefault()).getYear()
                : java.time.LocalDate.now().getYear();

        return new DonorDtos.DonorVerificationResponse(
                true,
                maskedName,
                profile.getBloodGroup(),
                profile.getEligibilityStatus(),
                profile.getTotalDonationsCount(),
                memberYear,
                user.getDonorUuid(),
                "Official BloodBridge Verified Donor Card"
        );
    }

    @Transactional(readOnly = true)
    public MyDonorProfileResponse getMine(String email) {
        return MyDonorProfileResponse.from(requireProfile(authService.requireByEmail(email)));
    }

    @Transactional
    public MyDonorProfileResponse saveMine(String email, UpsertDonorProfileRequest request) {
        UserAccount user = authService.requireByEmail(email);
        DonorProfile profile = donorProfiles.findByUser(user).orElseGet(() -> new DonorProfile(user));
        profile.update(
                request.bloodGroup(),
                request.phone().trim(),
                request.city().trim(),
                request.available(),
                request.lastDonationDate(),
                request.latitude(),
                request.longitude());
        return MyDonorProfileResponse.from(donorProfiles.save(profile));
    }

    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            return authService.requireByEmail(email).getId();
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<DonorMatchResponse> findAvailable(BloodGroup neededBloodGroup, String city) {
        String cityFilter = city == null ? "" : city.trim();
        return donorProfiles.findByAvailableTrueAndCityContainingIgnoreCase(cityFilter).stream()
                .filter(profile -> neededBloodGroup == null
                        || BloodCompatibility.canDonateTo(profile.getBloodGroup(), neededBloodGroup))
                .map(DonorMatchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DonorProfile requireProfile(UserAccount user) {
        return donorProfiles.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.PRECONDITION_REQUIRED, "Complete a donor profile before performing this action."));
    }
}
