package BloodBridge.donor;

import BloodBridge.common.BloodCompatibility;
import BloodBridge.common.BloodGroup;
import BloodBridge.common.LocationUtils;
import BloodBridge.donor.DonorDtos.SmartDonorMatchDto;
import BloodBridge.request.RequestUrgency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class SmartDonorMatchingService {

    private final DonorProfileRepository donorProfileRepository;

    @Value("${bloodbridge.matching.weight.compatibility:40}")
    private int compatibilityWeight;

    @Value("${bloodbridge.matching.weight.distance:25}")
    private int distanceWeight;

    @Value("${bloodbridge.matching.weight.availability:15}")
    private int availabilityWeight;

    @Value("${bloodbridge.matching.weight.eligibility:10}")
    private int eligibilityWeight;

    @Value("${bloodbridge.matching.weight.response-history:10}")
    private int responseHistoryWeight;

    public SmartDonorMatchingService(DonorProfileRepository donorProfileRepository) {
        this.donorProfileRepository = donorProfileRepository;
    }

    /**
     * Finds and ranks suitable donors based on multi-factor logistical criteria.
     * Disclaimer: This is a logistical ranking system, not a medical decision.
     */
    @Transactional(readOnly = true)
    public List<SmartDonorMatchDto> rankDonors(
            BloodGroup neededBloodGroup,
            String targetCity,
            Double targetLat,
            Double targetLon,
            RequestUrgency urgency,
            Long excludeUserId) {

        List<DonorProfile> allProfiles = donorProfileRepository.findAll();

        return allProfiles.stream()
                .filter(profile -> excludeUserId == null || !profile.getUser().getId().equals(excludeUserId))
                .filter(profile -> neededBloodGroup == null || BloodCompatibility.canDonateTo(profile.getBloodGroup(), neededBloodGroup))
                .map(profile -> scoreDonor(profile, neededBloodGroup, targetCity, targetLat, targetLon, urgency))
                .sorted(Comparator.comparingInt(SmartDonorMatchDto::matchScore).reversed())
                .toList();
    }

    private SmartDonorMatchDto scoreDonor(
            DonorProfile profile,
            BloodGroup neededBloodGroup,
            String targetCity,
            Double targetLat,
            Double targetLon,
            RequestUrgency urgency) {

        // 1. Compatibility Score (max: compatibilityWeight)
        double compScore = 0.0;
        String compatibilityLabel = "Compatible";
        if (neededBloodGroup != null) {
            if (profile.getBloodGroup() == neededBloodGroup) {
                compScore = compatibilityWeight;
                compatibilityLabel = "Exact Match";
            } else if (profile.getBloodGroup() == BloodGroup.O_NEGATIVE) {
                compScore = compatibilityWeight * 0.95;
                compatibilityLabel = "Universal Donor (O-)";
            } else if (BloodCompatibility.canDonateTo(profile.getBloodGroup(), neededBloodGroup)) {
                compScore = compatibilityWeight * 0.85;
                compatibilityLabel = "Compatible Type";
            }
        } else {
            compScore = compatibilityWeight * 0.8;
        }

        // 2. Distance Score (max: distanceWeight)
        Double distanceKm = null;
        if (targetLat != null && targetLon != null && profile.getLatitude() != null && profile.getLongitude() != null) {
            distanceKm = LocationUtils.calculateDistanceKm(targetLat, targetLon, profile.getLatitude(), profile.getLongitude());
        }

        double distScore = 0.0;
        if (distanceKm != null) {
            if (distanceKm <= 5.0) {
                distScore = distanceWeight;
            } else if (distanceKm <= 10.0) {
                distScore = distanceWeight * 0.85;
            } else if (distanceKm <= 25.0) {
                distScore = distanceWeight * 0.65;
            } else if (distanceKm <= 50.0) {
                distScore = distanceWeight * 0.40;
            } else {
                distScore = Math.max(0, distanceWeight * (1.0 - (distanceKm / 100.0)));
            }
        } else if (targetCity != null && !targetCity.isBlank() && profile.getCity() != null) {
            boolean sameCity = profile.getCity().trim().equalsIgnoreCase(targetCity.trim())
                    || profile.getCity().toLowerCase().contains(targetCity.toLowerCase())
                    || targetCity.toLowerCase().contains(profile.getCity().toLowerCase());
            distScore = sameCity ? distanceWeight * 0.80 : distanceWeight * 0.20;
        } else {
            distScore = distanceWeight * 0.50;
        }

        String distanceFormatted = LocationUtils.formatApproximateDistance(distanceKm, targetCity, profile.getCity());

        // 3. Availability Score (max: availabilityWeight)
        double availScore = profile.isAvailable() ? availabilityWeight : 0.0;

        // 4. Eligibility Score (max: eligibilityWeight)
        boolean isEligible = true;
        double eligScore = 0.0;

        if (profile.getEligibilityStatus() == EligibilityStatus.TEMPORARILY_UNAVAILABLE) {
            eligScore = eligibilityWeight * 0.2;
            isEligible = false;
        } else if (profile.getEligibilityStatus() == EligibilityStatus.REQUIRES_VERIFICATION) {
            eligScore = eligibilityWeight * 0.4;
            isEligible = false;
        } else {
            // Default ELIGIBLE
            if (profile.getLastDonationDate() != null) {
                long daysSinceLastDonation = ChronoUnit.DAYS.between(profile.getLastDonationDate(), LocalDate.now());
                if (daysSinceLastDonation >= 90) {
                    eligScore = eligibilityWeight;
                    isEligible = true;
                } else {
                    // Under 90-day whole blood cooldown
                    eligScore = eligibilityWeight * Math.max(0.1, (double) daysSinceLastDonation / 90.0);
                    isEligible = false;
                }
            } else {
                // First-time or unrecorded previous donation
                eligScore = eligibilityWeight * 0.9;
                isEligible = true;
            }
        }

        // 5. Response History Score (max: responseHistoryWeight)
        double respScore = 0.0;
        if (profile.getResponseCount() == 0) {
            respScore = responseHistoryWeight * 0.70; // Neutral baseline for new donors
        } else {
            double positiveRatio = (double) profile.getPositiveResponseCount() / profile.getResponseCount();
            respScore = responseHistoryWeight * (0.30 + (0.70 * positiveRatio));
        }

        // Urgency factor: Boost high/critical requests for ready donors
        double urgencyBonus = 0.0;
        if ((urgency == RequestUrgency.HIGH || urgency == RequestUrgency.CRITICAL) && profile.isAvailable() && isEligible) {
            urgencyBonus = 2.0;
        }

        int totalScore = (int) Math.round(compScore + distScore + availScore + eligScore + respScore + urgencyBonus);
        totalScore = Math.min(99, Math.max(10, totalScore));

        return new SmartDonorMatchDto(
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getBloodGroup(),
                profile.getCity(),
                totalScore,
                distanceKm,
                distanceFormatted,
                profile.isAvailable(),
                isEligible,
                profile.getEligibilityStatus(),
                compatibilityLabel,
                profile.getLastDonationDate());
    }
}
