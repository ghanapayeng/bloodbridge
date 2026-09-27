package BloodBridge.donor;

import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodGroup;
import BloodBridge.donor.DonorDtos.DonorEligibilityResponse;
import BloodBridge.notification.AppNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonorEligibilityServiceTest {

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private AppNotificationRepository appNotificationRepository;

    private DonorEligibilityService service;

    @BeforeEach
    void setUp() {
        service = new DonorEligibilityService(donorProfileRepository, appNotificationRepository);
        ReflectionTestUtils.setField(service, "wholeBloodCooldownDays", 90);
    }

    @Test
    void shouldBeEligibleWhenNeverDonated() {
        UserAccount user = new UserAccount("John Doe", "john@example.com", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(user, "id", 1L);

        DonorProfile profile = new DonorProfile(user);
        profile.update(BloodGroup.O_POSITIVE, "1234567890", "Delhi", true, null);
        when(donorProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));

        DonorEligibilityResponse response = service.evaluateEligibility(1L);

        assertThat(response.eligible()).isTrue();
        assertThat(response.status()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(response.daysRemaining()).isZero();
    }

    @Test
    void shouldBeInCooldownWhenDonatedRecently() {
        UserAccount user = new UserAccount("Jane Doe", "jane@example.com", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(user, "id", 2L);

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        DonorProfile profile = new DonorProfile(user);
        profile.update(BloodGroup.A_POSITIVE, "1234567890", "Delhi", true, thirtyDaysAgo);
        when(donorProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(profile));

        DonorEligibilityResponse response = service.evaluateEligibility(2L);

        assertThat(response.eligible()).isFalse();
        assertThat(response.status()).isEqualTo(EligibilityStatus.TEMPORARILY_UNAVAILABLE);
        assertThat(response.daysRemaining()).isGreaterThan(0);
        assertThat(response.nextEligibleDate()).isEqualTo(thirtyDaysAgo.plusDays(90));
    }
}
