package BloodBridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import BloodBridge.auth.AuthDtos.RegisterRequest;
import BloodBridge.auth.AuthService;
import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodGroup;
import BloodBridge.donation.DonationDtos.CreateDonation;
import BloodBridge.donation.DonationDtos.DonationResponse;
import BloodBridge.donation.DonationService;
import BloodBridge.donor.DonorDtos.DonorMatchResponse;
import BloodBridge.donor.DonorDtos.MyDonorProfileResponse;
import BloodBridge.donor.DonorDtos.UpsertDonorProfileRequest;
import BloodBridge.donor.DonorProfileService;
import BloodBridge.request.BloodRequestDtos.BloodRequestResponse;
import BloodBridge.request.BloodRequestDtos.CreateBloodRequest;
import BloodBridge.request.BloodRequestDtos.DonorInterestResponse;
import BloodBridge.request.BloodRequestDtos.MyDonorInterestResponse;
import BloodBridge.request.BloodRequestDtos.UpdateInterestStatus;
import BloodBridge.request.BloodRequestDtos.UpdateRequestStatus;
import BloodBridge.request.BloodRequestInterestStatus;
import BloodBridge.request.BloodRequestService;
import BloodBridge.request.BloodRequestStatus;
import BloodBridge.request.RequestUrgency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BloodBridgeIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private DonorProfileService donorProfileService;

    @Autowired
    private BloodRequestService bloodRequestService;

    @Autowired
    private DonationService donationService;

    private UserAccount requester;
    private UserAccount donor;

    @BeforeEach
    void setUp() {
        requester = authService.register(new RegisterRequest("Alice Requester", "alice@example.com", "SecurePassword123!"));
        donor = authService.register(new RegisterRequest("Bob Donor", "bob@example.com", "SecurePassword123!"));
    }

    @Test
    void testEndToEndDonorAndBloodRequestLifecycle() {
        // 1. Setup donor profile for Bob (O- universal donor)
        MyDonorProfileResponse profile = donorProfileService.saveMine(
                donor.getEmail(),
                new UpsertDonorProfileRequest(BloodGroup.O_NEGATIVE, "+91 99999 11111", "New Delhi", true, null));
        assertNotNull(profile);
        assertEquals(BloodGroup.O_NEGATIVE, profile.bloodGroup());
        assertTrue(profile.available());

        // 2. Query available donors for B+ in Delhi (O- should match B+)
        List<DonorMatchResponse> matchedDonors = donorProfileService.findAvailable(BloodGroup.B_POSITIVE, "Delhi");
        assertFalse(matchedDonors.isEmpty());
        assertTrue(matchedDonors.stream().anyMatch(d -> d.donorId().equals(donor.getId())));

        // 3. Alice creates a blood request for B+
        BloodRequestResponse request = bloodRequestService.create(
                requester.getEmail(),
                new CreateBloodRequest(BloodGroup.B_POSITIVE, "New Delhi", "City Hospital", RequestUrgency.HIGH, "Urgent surgery"));
        assertNotNull(request);
        assertEquals(BloodRequestStatus.OPEN, request.status());

        // 4. Bob registers interest in Alice's request
        bloodRequestService.registerInterest(donor.getEmail(), request.id());

        // 5. Bob can list his own expressed interests
        List<MyDonorInterestResponse> bobsInterests = bloodRequestService.listMyInterests(donor.getEmail());
        assertEquals(1, bobsInterests.size());
        assertEquals(BloodRequestInterestStatus.PENDING, bobsInterests.get(0).interestStatus());
        assertEquals(request.id(), bobsInterests.get(0).requestId());

        // 6. Alice views interests for her request
        List<DonorInterestResponse> aliceViewsInterests = bloodRequestService.listInterests(requester.getEmail(), request.id());
        assertEquals(1, aliceViewsInterests.size());
        assertEquals("Bob Donor", aliceViewsInterests.get(0).donorName());
        assertEquals("+91 99999 11111", aliceViewsInterests.get(0).donorPhone());

        // 7. Alice accepts Bob's interest
        Long interestId = aliceViewsInterests.get(0).id();
        DonorInterestResponse updatedInterest = bloodRequestService.updateInterestStatus(
                requester.getEmail(),
                request.id(),
                interestId,
                new UpdateInterestStatus(BloodRequestInterestStatus.ACCEPTED));
        assertEquals(BloodRequestInterestStatus.ACCEPTED, updatedInterest.status());

        // 8. Bob records the completed donation linked to this request
        DonationResponse donation = donationService.record(
                donor.getEmail(),
                new CreateDonation(request.id(), LocalDate.now(), new BigDecimal("1.0")));
        assertNotNull(donation);
        assertEquals(new BigDecimal("1.0"), donation.units());
        assertEquals(request.id(), donation.bloodRequestId());

        // Verify Bob's profile was automatically updated with last donation date
        MyDonorProfileResponse updatedProfile = donorProfileService.getMine(donor.getEmail());
        assertEquals(LocalDate.now(), updatedProfile.lastDonationDate());

        // 9. Alice marks request fulfilled
        BloodRequestResponse fulfilledReq = bloodRequestService.updateStatus(
                requester.getEmail(),
                request.id(),
                new UpdateRequestStatus(BloodRequestStatus.FULFILLED));
        assertEquals(BloodRequestStatus.FULFILLED, fulfilledReq.status());

        // 10. Bob views his donation history
        List<DonationResponse> bobsDonations = donationService.listMine(donor.getEmail());
        assertEquals(1, bobsDonations.size());
        assertEquals("City Hospital", bobsDonations.get(0).hospital());
    }
}
