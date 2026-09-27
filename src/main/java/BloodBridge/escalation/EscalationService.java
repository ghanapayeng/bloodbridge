package BloodBridge.escalation;

import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.common.BloodCompatibility;
import BloodBridge.common.LocationUtils;
import BloodBridge.donor.DonorProfile;
import BloodBridge.donor.DonorProfileRepository;
import BloodBridge.escalation.EscalationDtos.EscalationLogResponse;
import BloodBridge.escalation.EscalationDtos.EscalationResultResponse;
import BloodBridge.notification.AppNotification;
import BloodBridge.notification.AppNotificationRepository;
import BloodBridge.notification.NotificationType;
import BloodBridge.request.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private static final double[] RADII_KM = {5.0, 10.0, 25.0, 50.0, 100.0};

    private final BloodRequestRepository bloodRequestRepository;
    private final RequestEscalationLogRepository escalationLogRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final UserAccountRepository userAccountRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final RequestTimelineEventRepository requestTimelineEventRepository;

    public EscalationService(
            BloodRequestRepository bloodRequestRepository,
            RequestEscalationLogRepository escalationLogRepository,
            DonorProfileRepository donorProfileRepository,
            UserAccountRepository userAccountRepository,
            AppNotificationRepository appNotificationRepository,
            RequestTimelineEventRepository requestTimelineEventRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.escalationLogRepository = escalationLogRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.userAccountRepository = userAccountRepository;
        this.appNotificationRepository = appNotificationRepository;
        this.requestTimelineEventRepository = requestTimelineEventRepository;
    }

    @Transactional
    public EscalationResultResponse escalateRequest(Long requestId) {
        BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Blood request not found with ID: " + requestId));

        if (request.getStatus() != BloodRequestStatus.OPEN) {
            throw new IllegalStateException("Cannot escalate a request that is " + request.getStatus());
        }

        List<RequestEscalationLog> existingLogs = escalationLogRepository.findByBloodRequest_IdOrderByEscalatedAtAsc(requestId);
        int currentStep = existingLogs.size();

        if (currentStep >= RADII_KM.length) {
            // Already at maximum escalation (hospital level)
            return new EscalationResultResponse(
                    requestId,
                    currentStep,
                    RADII_KM[RADII_KM.length - 1],
                    null,
                    0,
                    existingLogs.stream().mapToInt(RequestEscalationLog::getDonorsNotifiedCount).sum(),
                    true,
                    "Request is already at maximum inter-hospital escalation."
            );
        }

        double radius = RADII_KM[currentStep];
        int stepNumber = currentStep + 1;
        Double nextRadius = (stepNumber < RADII_KM.length) ? RADII_KM[stepNumber] : null;
        boolean isHospitalLevel = (stepNumber == RADII_KM.length);

        // Find users already notified for this request to avoid re-notifying them
        Set<Long> alreadyNotifiedUserIds = appNotificationRepository.findByRelatedRequestId(requestId).stream()
                .map(n -> n.getRecipient().getId())
                .collect(Collectors.toCollection(HashSet::new));

        alreadyNotifiedUserIds.add(request.getRequester().getId());

        int newlyNotifiedCount = 0;
        String actionSummary;

        if (isHospitalLevel) {
            // Step 5: Notify partner hospitals & blood bank coordinators
            List<UserAccount> hospitals = userAccountRepository.findByRole("ROLE_HOSPITAL");
            for (UserAccount hospital : hospitals) {
                if (!alreadyNotifiedUserIds.contains(hospital.getId())) {
                    appNotificationRepository.save(new AppNotification(
                            hospital,
                            NotificationType.TRANSFER_REQUEST,
                            "Urgent Hospital Escalation: " + request.getBloodGroup() + " Needed",
                            "Emergency escalation: " + request.getHospital() + " urgently requires "
                                    + request.getBloodGroup() + " blood units (" + request.getUnitsNeeded()
                                    + " needed). Please check inter-facility reserve.",
                            requestId
                    ));
                    alreadyNotifiedUserIds.add(hospital.getId());
                    newlyNotifiedCount++;
                }
            }
            actionSummary = "Escalated to inter-hospital blood bank network. Notified "
                    + newlyNotifiedCount + " facility coordinators.";
        } else {
            // Steps 1-4: Search radius escalation for donors
            List<DonorProfile> candidateDonors = donorProfileRepository.findAll().stream()
                    .filter(DonorProfile::isAvailable)
                    .filter(d -> BloodCompatibility.canDonateTo(d.getBloodGroup(), request.getBloodGroup()))
                    .filter(d -> !alreadyNotifiedUserIds.contains(d.getUser().getId()))
                    .filter(d -> isWithinRadius(request, d, radius))
                    .toList();

            for (DonorProfile donor : candidateDonors) {
                appNotificationRepository.save(new AppNotification(
                        donor.getUser(),
                        NotificationType.EMERGENCY_REQUEST,
                        "Emergency Escalation: " + request.getBloodGroup() + " Needed Nearby",
                        "Search radius expanded to " + (int) radius + " km. Urgent blood needed at "
                                + request.getHospital() + " in " + request.getCity() + ". Can you help save a life?",
                        requestId
                ));
                alreadyNotifiedUserIds.add(donor.getUser().getId());
                newlyNotifiedCount++;
            }

            actionSummary = "Escalated to " + (int) radius + " km search radius. Contacted "
                    + newlyNotifiedCount + " newly reachable donors.";
        }

        // Save escalation log
        RequestEscalationLog escalationLog = new RequestEscalationLog(
                request,
                radius,
                newlyNotifiedCount,
                actionSummary
        );
        escalationLogRepository.save(escalationLog);

        // Record on request visual timeline
        requestTimelineEventRepository.save(new RequestTimelineEvent(
                request,
                RequestStage.DONORS_CONTACTED,
                "Escalation Level " + stepNumber + " (" + (isHospitalLevel ? "Hospitals" : (int) radius + " km") + "): " + actionSummary,
                "SYSTEM"
        ));

        int totalNotified = existingLogs.stream().mapToInt(RequestEscalationLog::getDonorsNotifiedCount).sum() + newlyNotifiedCount;

        return new EscalationResultResponse(
                requestId,
                stepNumber,
                radius,
                nextRadius,
                newlyNotifiedCount,
                totalNotified,
                isHospitalLevel,
                actionSummary
        );
    }

    @Transactional(readOnly = true)
    public List<EscalationLogResponse> getEscalationHistory(Long requestId) {
        return escalationLogRepository.findByBloodRequest_IdOrderByEscalatedAtAsc(requestId).stream()
                .map(EscalationLogResponse::fromEntity)
                .toList();
    }

    /**
     * Automatic escalation scheduler: checks open critical or urgent requests older than 30 minutes
     * that haven't reached full fulfillment or final escalation.
     */
    @Scheduled(cron = "${bloodbridge.escalation.cron:0 */15 * * * *}")
    @Transactional
    public void processAutoEscalations() {
        Instant thirtyMinsAgo = Instant.now().minus(Duration.ofMinutes(30));
        List<BloodRequest> openEmergencies = bloodRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == BloodRequestStatus.OPEN)
                .filter(r -> r.getUrgency() == RequestUrgency.CRITICAL || r.getUrgency() == RequestUrgency.URGENT)
                .filter(r -> r.getCreatedAt().isBefore(thirtyMinsAgo))
                .filter(r -> r.getUnitsFulfilled() < r.getUnitsNeeded())
                .toList();

        for (BloodRequest emergency : openEmergencies) {
            try {
                List<RequestEscalationLog> logs = escalationLogRepository.findByBloodRequest_IdOrderByEscalatedAtAsc(emergency.getId());
                if (logs.size() < RADII_KM.length) {
                    // Check time of last escalation: only escalate if last escalation was > 20 mins ago
                    if (logs.isEmpty() || logs.get(logs.size() - 1).getEscalatedAt().isBefore(Instant.now().minus(Duration.ofMinutes(20)))) {
                        log.info("Auto-escalating emergency request ID: {}", emergency.getId());
                        escalateRequest(emergency.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("Auto-escalation failed for request {}: {}", emergency.getId(), e.getMessage());
            }
        }
    }

    private boolean isWithinRadius(BloodRequest request, DonorProfile donor, double radiusKm) {
        if (request.getLatitude() != null && request.getLongitude() != null
                && donor.getLatitude() != null && donor.getLongitude() != null) {
            double distance = LocationUtils.calculateDistanceKm(
                    request.getLatitude(), request.getLongitude(),
                    donor.getLatitude(), donor.getLongitude()
            );
            return distance <= radiusKm;
        }

        // Fallback: If no coordinates, consider same city within first 25 km
        if (radiusKm >= 10.0 && request.getCity() != null && donor.getCity() != null) {
            return request.getCity().trim().equalsIgnoreCase(donor.getCity().trim())
                    || request.getCity().toLowerCase().contains(donor.getCity().toLowerCase())
                    || donor.getCity().toLowerCase().contains(request.getCity().toLowerCase());
        }

        return false;
    }
}
