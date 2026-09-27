package BloodBridge.request;

import BloodBridge.auth.AuthService;
import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodCompatibility;
import BloodBridge.common.BloodGroup;
import BloodBridge.donor.DonorProfile;
import BloodBridge.donor.DonorProfileRepository;
import BloodBridge.donor.DonorProfileService;
import BloodBridge.notification.AppNotification;
import BloodBridge.notification.AppNotificationRepository;
import BloodBridge.notification.NotificationType;
import BloodBridge.request.BloodRequestDtos.BloodRequestResponse;
import BloodBridge.request.BloodRequestDtos.CreateBloodRequest;
import BloodBridge.audit.AuditLogService;
import BloodBridge.request.BloodRequestDtos.DonorInterestResponse;
import BloodBridge.request.BloodRequestDtos.RiskReviewRequest;
import BloodBridge.request.BloodRequestDtos.TimelineEventResponse;
import BloodBridge.request.BloodRequestDtos.UpdateInterestStatus;
import BloodBridge.request.BloodRequestDtos.UpdateRequestStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BloodRequestService {

    private final BloodRequestRepository bloodRequests;
    private final BloodRequestInterestRepository interests;
    private final AuthService authService;
    private final DonorProfileService donorProfiles;
    private final DonorProfileRepository donorProfilesRepo;
    private final RequestTimelineEventRepository timelineEvents;
    private final AppNotificationRepository notifications;
    private final RequestFraudDetectionService fraudDetectionService;
    private final AuditLogService auditLogService;

    public BloodRequestService(
            BloodRequestRepository bloodRequests,
            BloodRequestInterestRepository interests,
            AuthService authService,
            DonorProfileService donorProfiles,
            DonorProfileRepository donorProfilesRepo,
            RequestTimelineEventRepository timelineEvents,
            AppNotificationRepository notifications,
            RequestFraudDetectionService fraudDetectionService,
            AuditLogService auditLogService) {
        this.bloodRequests = bloodRequests;
        this.interests = interests;
        this.authService = authService;
        this.donorProfiles = donorProfiles;
        this.donorProfilesRepo = donorProfilesRepo;
        this.timelineEvents = timelineEvents;
        this.notifications = notifications;
        this.fraudDetectionService = fraudDetectionService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public BloodRequestResponse create(String email, CreateBloodRequest payload) {
        UserAccount requester = authService.requireByEmail(email);
        BloodRequest bloodRequest = new BloodRequest(
                requester,
                payload.bloodGroup(),
                payload.city().trim(),
                payload.hospital().trim(),
                payload.urgency(),
                emptyToNull(payload.additionalMessage()),
                payload.unitsNeeded(),
                payload.patientReference(),
                payload.requiredBy(),
                payload.latitude(),
                payload.longitude());

        RequestFraudDetectionService.RiskAssessmentResult riskAssessment = fraudDetectionService.assessRequest(
                requester,
                payload.bloodGroup(),
                payload.hospital(),
                payload.patientReference());

        if (riskAssessment.isFlagged()) {
            bloodRequest.setRiskLevel(riskAssessment.riskLevel());
        }

        BloodRequest saved = bloodRequests.save(bloodRequest);

        // Record initial timeline event
        timelineEvents.save(new RequestTimelineEvent(
                saved,
                RequestStage.REQUEST_CREATED,
                "Blood request submitted for " + saved.getBloodGroup().getDisplay() + " (" + saved.getUnitsNeeded() + " unit" + (saved.getUnitsNeeded() > 1 ? "s" : "") + ") at " + saved.getHospital(),
                requester.getFullName()));

        if (riskAssessment.isFlagged()) {
            timelineEvents.save(new RequestTimelineEvent(
                    saved,
                    RequestStage.REQUEST_CREATED,
                    "Security Flag (" + riskAssessment.riskLevel() + "): " + String.join("; ", riskAssessment.flags()),
                    "BloodBridge Safety Engine"));

            auditLogService.logAction(
                    requester.getEmail(),
                    "REQUEST_SECURITY_FLAG",
                    "BloodRequest",
                    saved.getId(),
                    riskAssessment.riskLevel().name(),
                    String.join("; ", riskAssessment.flags()));
        }

        // For URGENT and CRITICAL requests: trigger donor matching and notification
        if (saved.getUrgency() != null && saved.getUrgency().isEmergency() && saved.getRiskLevel() != RequestRiskLevel.HIGH_RISK) {
            notifyEmergencyDonors(saved);
        }

        return BloodRequestResponse.from(saved);
    }

    private void notifyEmergencyDonors(BloodRequest request) {
        request.setCurrentStage(RequestStage.MATCHING_DONORS);
        String targetCity = request.getCity();

        List<DonorProfile> compatibleDonors = donorProfilesRepo.findAll().stream()
                .filter(DonorProfile::isAvailable)
                .filter(p -> !p.getUser().getId().equals(request.getRequester().getId()))
                .filter(p -> BloodCompatibility.canDonateTo(p.getBloodGroup(), request.getBloodGroup()))
                .filter(p -> targetCity == null || targetCity.isBlank() || p.getCity() == null || p.getCity().toLowerCase().contains(targetCity.toLowerCase()) || targetCity.toLowerCase().contains(p.getCity().toLowerCase()))
                .toList();

        int notifiedCount = 0;
        for (DonorProfile donor : compatibleDonors) {
            notifications.save(new AppNotification(
                    donor.getUser(),
                    NotificationType.EMERGENCY_REQUEST,
                    "🚨 Urgent Blood Needed: " + request.getBloodGroup().getDisplay() + " at " + request.getHospital(),
                    "Immediate requirement for " + request.getUnitsNeeded() + " unit(s) of " + request.getBloodGroup().getDisplay() + " in " + request.getCity() + ". Tap to help save a life.",
                    request.getId()));
            notifiedCount++;
        }

        if (notifiedCount > 0) {
            request.setCurrentStage(RequestStage.DONORS_CONTACTED);
            timelineEvents.save(new RequestTimelineEvent(
                    request,
                    RequestStage.DONORS_CONTACTED,
                    "Alerted " + notifiedCount + " nearby compatible donor(s) via emergency dispatch",
                    "System Automated Dispatcher"));
        } else {
            timelineEvents.save(new RequestTimelineEvent(
                    request,
                    RequestStage.MATCHING_DONORS,
                    "Searching for additional compatible donors in expanded regional zone",
                    "System Automated Dispatcher"));
        }
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listOpen(BloodGroup bloodGroup, String city) {
        String cityFilter = city == null ? "" : city.trim().toLowerCase();
        return bloodRequests.findByStatusOrderByCreatedAtDesc(BloodRequestStatus.OPEN).stream()
                .filter(request -> bloodGroup == null || request.getBloodGroup() == bloodGroup)
                .filter(request -> cityFilter.isBlank() || request.getCity().toLowerCase().contains(cityFilter))
                .map(BloodRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listEmergencies(BloodGroup bloodGroup, String city) {
        String cityFilter = city == null ? "" : city.trim().toLowerCase();
        return bloodRequests.findByStatusOrderByCreatedAtDesc(BloodRequestStatus.OPEN).stream()
                .filter(request -> request.getUrgency() != null && request.getUrgency().isEmergency())
                .filter(request -> bloodGroup == null || request.getBloodGroup() == bloodGroup)
                .filter(request -> cityFilter.isBlank() || request.getCity().toLowerCase().contains(cityFilter))
                .map(BloodRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> listMine(String email) {
        return bloodRequests.findByRequesterOrderByCreatedAtDesc(authService.requireByEmail(email)).stream()
                .map(BloodRequestResponse::from)
                .toList();
    }

    @Transactional
    public void registerInterest(String email, long requestId) {
        UserAccount donor = authService.requireByEmail(email);
        BloodRequest bloodRequest = requireRequest(requestId);
        if (bloodRequest.getRequester().getId().equals(donor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot respond to your own blood request.");
        }
        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This blood request is no longer open.");
        }
        if (interests.existsByBloodRequest_IdAndDonor_Id(requestId, donor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already responded to this request.");
        }

        DonorProfile profile = donorProfiles.requireProfile(donor);
        if (!profile.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Set your donor profile to available before responding.");
        }
        if (!BloodCompatibility.canDonateTo(profile.getBloodGroup(), bloodRequest.getBloodGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your blood group is not compatible with this request.");
        }

        interests.save(new BloodRequestInterest(bloodRequest, donor));

        // Track response on profile
        profile.incrementResponseCount(true);
        donorProfilesRepo.save(profile);

        // Notify requester that a donor volunteered
        notifications.save(new AppNotification(
                bloodRequest.getRequester(),
                NotificationType.DONOR_RESPONSE,
                "Volunteer response received from " + donor.getFullName(),
                donor.getFullName() + " (" + profile.getBloodGroup().getDisplay() + ") has stepped up to help with your request at " + bloodRequest.getHospital(),
                bloodRequest.getId()));
    }

    @Transactional(readOnly = true)
    public List<DonorInterestResponse> listInterests(String email, long requestId) {
        BloodRequest bloodRequest = requireOwnedRequest(email, requestId);
        return interests.findByBloodRequestOrderByCreatedAtDesc(bloodRequest).stream()
                .map(interest -> DonorInterestResponse.from(
                        interest,
                        donorProfiles.requireProfile(interest.getDonor())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloodRequestDtos.MyDonorInterestResponse> listMyInterests(String email) {
        UserAccount donor = authService.requireByEmail(email);
        return interests.findByDonorOrderByCreatedAtDesc(donor).stream()
                .map(BloodRequestDtos.MyDonorInterestResponse::from)
                .toList();
    }

    @Transactional
    public BloodRequestResponse updateStatus(String email, long requestId, UpdateRequestStatus payload) {
        BloodRequest bloodRequest = requireOwnedRequest(email, requestId);
        bloodRequest.setStatus(payload.status());

        if (payload.status() == BloodRequestStatus.FULFILLED) {
            bloodRequest.setCurrentStage(RequestStage.FULFILLED);
            timelineEvents.save(new RequestTimelineEvent(
                    bloodRequest,
                    RequestStage.FULFILLED,
                    "Blood request fulfilled successfully",
                    bloodRequest.getRequester().getFullName()));
        }

        return BloodRequestResponse.from(bloodRequest);
    }

    @Transactional
    public BloodRequestResponse updateFulfillment(String email, long requestId, int unitsFulfilled) {
        BloodRequest bloodRequest = requireOwnedRequest(email, requestId);
        bloodRequest.setUnitsFulfilled(unitsFulfilled);

        if (unitsFulfilled >= bloodRequest.getUnitsNeeded()) {
            bloodRequest.setStatus(BloodRequestStatus.FULFILLED);
            bloodRequest.setCurrentStage(RequestStage.FULFILLED);
            timelineEvents.save(new RequestTimelineEvent(
                    bloodRequest,
                    RequestStage.FULFILLED,
                    "All " + bloodRequest.getUnitsNeeded() + " units fulfilled!",
                    bloodRequest.getRequester().getFullName()));
        }

        return BloodRequestResponse.from(bloodRequest);
    }

    @Transactional
    public DonorInterestResponse updateInterestStatus(
            String email,
            long requestId,
            long interestId,
            UpdateInterestStatus payload) {
        if (payload.status() != BloodRequestInterestStatus.ACCEPTED
                && payload.status() != BloodRequestInterestStatus.DECLINED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An interest can only be accepted or declined by the requester.");
        }
        BloodRequest bloodRequest = requireOwnedRequest(email, requestId);
        BloodRequestInterest interest = interests.findById(interestId)
                .filter(found -> found.getBloodRequest().getId().equals(bloodRequest.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor interest not found."));

        interest.setStatus(payload.status());

        if (payload.status() == BloodRequestInterestStatus.ACCEPTED) {
            bloodRequest.setCurrentStage(RequestStage.DONOR_CONFIRMED);
            timelineEvents.save(new RequestTimelineEvent(
                    bloodRequest,
                    RequestStage.DONOR_CONFIRMED,
                    "Donor confirmed: " + interest.getDonor().getFullName() + " accepted for donation",
                    bloodRequest.getRequester().getFullName()));

            // Notify the donor
            notifications.save(new AppNotification(
                    interest.getDonor(),
                    NotificationType.STATUS_UPDATE,
                    "Donation Confirmed for " + bloodRequest.getHospital(),
                    "The requester accepted your offer to donate. Please coordinate via direct chat or phone call.",
                    bloodRequest.getId()));
        }

        return DonorInterestResponse.from(interest, donorProfiles.requireProfile(interest.getDonor()));
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimeline(long requestId) {
        requireRequest(requestId);
        return timelineEvents.findByBloodRequest_IdOrderByCreatedAtAsc(requestId).stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    private BloodRequest requireOwnedRequest(String email, long requestId) {
        UserAccount requester = authService.requireByEmail(email);
        BloodRequest bloodRequest = requireRequest(requestId);
        if (!bloodRequest.getRequester().getId().equals(requester.getId()) && !requester.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this blood request.");
        }
        return bloodRequest;
    }

    public BloodRequest requireRequest(long requestId) {
        return bloodRequests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found."));
    }

    @Transactional(readOnly = true)
    public List<BloodRequestResponse> getFlaggedRequests() {
        return bloodRequests.findByRiskLevelNotOrderByCreatedAtDesc(RequestRiskLevel.NORMAL).stream()
                .map(BloodRequestResponse::from)
                .toList();
    }

    @Transactional
    public BloodRequestResponse reviewRisk(Long requestId, RiskReviewRequest payload, String reviewerEmail) {
        BloodRequest bloodRequest = requireRequest(requestId);
        if (payload.approved()) {
            bloodRequest.setRiskLevel(RequestRiskLevel.NORMAL);
            timelineEvents.save(new RequestTimelineEvent(
                    bloodRequest,
                    bloodRequest.getCurrentStage(),
                    "Risk flag cleared by " + reviewerEmail + (payload.notes() != null && !payload.notes().isBlank() ? ": " + payload.notes() : ""),
                    reviewerEmail));

            auditLogService.logAction(
                    reviewerEmail,
                    "REQUEST_RISK_CLEARED",
                    "BloodRequest",
                    bloodRequest.getId(),
                    "APPROVED",
                    payload.notes());
        } else {
            bloodRequest.setStatus(BloodRequestStatus.CANCELLED);
            timelineEvents.save(new RequestTimelineEvent(
                    bloodRequest,
                    bloodRequest.getCurrentStage(),
                    "Request rejected and cancelled following risk review by " + reviewerEmail + (payload.notes() != null && !payload.notes().isBlank() ? ": " + payload.notes() : ""),
                    reviewerEmail));

            auditLogService.logAction(
                    reviewerEmail,
                    "REQUEST_RISK_REJECTED",
                    "BloodRequest",
                    bloodRequest.getId(),
                    "REJECTED",
                    payload.notes());
        }
        return BloodRequestResponse.from(bloodRequest);
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
