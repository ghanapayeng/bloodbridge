package BloodBridge.request;

import BloodBridge.auth.UserAccount;
import BloodBridge.common.BloodGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestFraudDetectionServiceTest {

    @Mock
    private BloodRequestRepository bloodRequestRepository;

    private RequestFraudDetectionService service;

    @BeforeEach
    void setUp() {
        service = new RequestFraudDetectionService(bloodRequestRepository);
    }

    @Test
    void shouldAssessNormalForFirstTimeValidRequest() {
        UserAccount requester = new UserAccount("Requester", "req@example.com", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(requester, "id", 10L);

        when(bloodRequestRepository.findByBloodGroupAndPatientReferenceIgnoreCaseAndStatus(
                BloodGroup.O_POSITIVE, "PAT-1234", BloodRequestStatus.OPEN))
                .thenReturn(Collections.emptyList());

        when(bloodRequestRepository.findByRequesterAndCreatedAtAfter(eq(requester), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        RequestFraudDetectionService.RiskAssessmentResult result = service.assessRequest(
                requester, BloodGroup.O_POSITIVE, "City Hospital", "PAT-1234");

        assertThat(result.riskLevel()).isEqualTo(RequestRiskLevel.NORMAL);
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void shouldFlagHighRiskWhenDuplicateActiveRequestExists() {
        UserAccount requester = new UserAccount("Requester", "req@example.com", "hash", "ROLE_USER");
        ReflectionTestUtils.setField(requester, "id", 10L);

        BloodRequest existing = new BloodRequest(
                requester, BloodGroup.B_POSITIVE, "Delhi", "City Hospital", RequestUrgency.NORMAL, null);
        ReflectionTestUtils.setField(existing, "patientReference", "PAT-DUPLICATE");

        when(bloodRequestRepository.findByBloodGroupAndPatientReferenceIgnoreCaseAndStatus(
                BloodGroup.B_POSITIVE, "PAT-DUPLICATE", BloodRequestStatus.OPEN))
                .thenReturn(List.of(existing));

        when(bloodRequestRepository.findByRequesterAndCreatedAtAfter(eq(requester), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        RequestFraudDetectionService.RiskAssessmentResult result = service.assessRequest(
                requester, BloodGroup.B_POSITIVE, "City Hospital", "PAT-DUPLICATE");

        assertThat(result.riskLevel()).isEqualTo(RequestRiskLevel.HIGH_RISK);
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.flags()).anyMatch(f -> f.contains("Duplicate alert"));
    }
}
