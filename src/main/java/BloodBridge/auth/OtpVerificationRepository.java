package BloodBridge.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(String identifier, String otpType);

    void deleteByIdentifierAndOtpType(String identifier, String otpType);
}
