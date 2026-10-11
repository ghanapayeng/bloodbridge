package BloodBridge.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(String identifier, String otpType);

    long countByIdentifierAndOtpTypeAndCreatedAtAfter(String identifier, String otpType, Instant after);

    void deleteByIdentifierAndOtpType(String identifier, String otpType);
}
