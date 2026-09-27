package BloodBridge.donor;

import BloodBridge.donor.DonorDtos.DonorEligibilityResponse;
import BloodBridge.notification.AppNotification;
import BloodBridge.notification.AppNotificationRepository;
import BloodBridge.notification.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class DonorEligibilityService {

    private static final String LOGISTICAL_DISCLAIMER =
            "This eligibility calculation is a logistical estimation based on recorded whole-blood donation intervals. "
            + "It does not replace clinical pre-screening (hemoglobin, vitals, medical history) conducted at the blood collection center.";

    private final DonorProfileRepository donorProfileRepository;
    private final AppNotificationRepository appNotificationRepository;

    @Value("${bloodbridge.eligibility.cooldown-days:90}")
    private int wholeBloodCooldownDays;

    public DonorEligibilityService(
            DonorProfileRepository donorProfileRepository,
            AppNotificationRepository appNotificationRepository) {
        this.donorProfileRepository = donorProfileRepository;
        this.appNotificationRepository = appNotificationRepository;
    }

    @Transactional
    public DonorEligibilityResponse evaluateEligibility(Long userId) {
        DonorProfile profile = donorProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("No donor profile found for user ID: " + userId));
        return evaluateEligibility(profile);
    }

    @Transactional
    public DonorEligibilityResponse evaluateEligibility(DonorProfile profile) {
        LocalDate today = LocalDate.now();
        LocalDate lastDonation = profile.getLastDonationDate();
        EligibilityStatus currentStatus = profile.getEligibilityStatus();

        if (currentStatus == EligibilityStatus.REQUIRES_VERIFICATION) {
            return new DonorEligibilityResponse(
                    profile.getUser().getId(),
                    false,
                    EligibilityStatus.REQUIRES_VERIFICATION,
                    lastDonation,
                    profile.getNextEligibleDate(),
                    0,
                    wholeBloodCooldownDays,
                    "Your donor profile or medical screening requires administrative/clinical verification before donating.",
                    LOGISTICAL_DISCLAIMER
            );
        }

        // Check whole blood donation interval
        if (lastDonation != null) {
            LocalDate nextEligible = lastDonation.plusDays(wholeBloodCooldownDays);
            profile.setNextEligibleDate(nextEligible);

            if (today.isBefore(nextEligible)) {
                long daysRemaining = ChronoUnit.DAYS.between(today, nextEligible);
                if (currentStatus != EligibilityStatus.TEMPORARILY_UNAVAILABLE) {
                    profile.setEligibilityStatus(EligibilityStatus.TEMPORARILY_UNAVAILABLE);
                    donorProfileRepository.save(profile);
                }
                return new DonorEligibilityResponse(
                        profile.getUser().getId(),
                        false,
                        EligibilityStatus.TEMPORARILY_UNAVAILABLE,
                        lastDonation,
                        nextEligible,
                        daysRemaining,
                        wholeBloodCooldownDays,
                        "You are currently in the whole-blood donation cooldown period. You will be eligible again on "
                                + nextEligible + " (" + daysRemaining + " days remaining).",
                        LOGISTICAL_DISCLAIMER
                );
            } else {
                // Cooldown period elapsed, restore to ELIGIBLE if was TEMPORARILY_UNAVAILABLE
                if (currentStatus == EligibilityStatus.TEMPORARILY_UNAVAILABLE) {
                    profile.setEligibilityStatus(EligibilityStatus.ELIGIBLE);
                    donorProfileRepository.save(profile);
                }
            }
        } else {
            // First time donor or no past record
            if (currentStatus != EligibilityStatus.ELIGIBLE) {
                profile.setEligibilityStatus(EligibilityStatus.ELIGIBLE);
                donorProfileRepository.save(profile);
            }
        }

        return new DonorEligibilityResponse(
                profile.getUser().getId(),
                true,
                EligibilityStatus.ELIGIBLE,
                lastDonation,
                profile.getNextEligibleDate(),
                0,
                wholeBloodCooldownDays,
                "You meet the logistical donation interval criteria and are eligible to donate.",
                LOGISTICAL_DISCLAIMER
        );
    }

    @Transactional
    public void recordCompletedDonation(DonorProfile profile, LocalDate donationDate) {
        if (donationDate == null) {
            donationDate = LocalDate.now();
        }
        profile.setLastDonationDate(donationDate);
        profile.setTotalDonationsCount(profile.getTotalDonationsCount() + 1);
        LocalDate nextEligible = donationDate.plusDays(wholeBloodCooldownDays);
        profile.setNextEligibleDate(nextEligible);
        profile.setEligibilityStatus(EligibilityStatus.TEMPORARILY_UNAVAILABLE);
        donorProfileRepository.save(profile);

        // Notify donor of gratitude and next eligibility date
        appNotificationRepository.save(new AppNotification(
                profile.getUser(),
                NotificationType.ELIGIBILITY_UPDATE,
                "Thank You for Your Donation!",
                "Your donation has been confirmed! You have helped save lives. Under safe donation guidelines, your next eligible date is "
                        + nextEligible + ".",
                null
        ));
    }

    @Transactional
    public DonorProfile updateEligibilityStatus(Long userId, EligibilityStatus newStatus) {
        DonorProfile profile = donorProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("Donor profile not found for user ID: " + userId));
        profile.setEligibilityStatus(newStatus);
        return donorProfileRepository.save(profile);
    }
}
