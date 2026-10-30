package BloodBridge.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findFirstByIdentifierAndOtpTypeAndPurposeOrderByCreatedAtDesc(
            String identifier, String otpType, String purpose);

    Optional<OtpVerification> findTopByIdentifierAndOtpTypeAndPurposeOrderByCreatedAtDesc(
            String identifier, String otpType, String purpose);

    long countByIdentifierAndOtpTypeAndPurposeAndCreatedAtAfter(
            String identifier, String otpType, String purpose, Instant after);

    void deleteByIdentifierAndOtpTypeAndPurpose(String identifier, String otpType, String purpose);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.verified = true WHERE o.id = :id AND o.verified = false")
    int markVerifiedAtomically(@Param("id") Long id);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.failedAttempts = o.failedAttempts + 1 WHERE o.id = :id")
    int incrementFailedAttempts(@Param("id") Long id);

    // Backward-compatible methods
    Optional<OtpVerification> findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(String identifier, String otpType);

    long countByIdentifierAndOtpTypeAndCreatedAtAfter(String identifier, String otpType, Instant after);

    void deleteByIdentifierAndOtpType(String identifier, String otpType);
}
