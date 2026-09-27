package BloodBridge.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final OtpVerificationRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpVerificationRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Transactional
    public String sendOtp(String rawIdentifier, String type) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        otpRepository.deleteByIdentifierAndOtpType(identifier, type);
        OtpVerification verification = new OtpVerification(identifier, code, type, expiresAt);
        otpRepository.save(verification);

        log.info("[OTP NOTIFICATION] Generated 6-digit code for {} ({}): {}", identifier, type, code);
        return code;
    }

    @Transactional
    public boolean verifyOtp(String rawIdentifier, String type, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        OtpVerification verification = otpRepository
                .findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(identifier, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No verification code was requested for this identifier."));

        if (verification.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has expired. Please request a new one.");
        }

        if (!verification.getOtpCode().equals(code.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code entered.");
        }

        verification.setVerified(true);
        otpRepository.save(verification);
        log.info("[OTP VERIFIED] Successfully verified {} ({})", identifier, type);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isVerified(String rawIdentifier, String type) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        return otpRepository
                .findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(identifier, type)
                .map(v -> v.isVerified() && !v.isExpired())
                .orElse(false);
    }

    private String normalizeIdentifier(String identifier, String type) {
        if (identifier == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifier is required.");
        }
        if ("EMAIL".equalsIgnoreCase(type) || identifier.contains("@")) {
            return identifier.trim().toLowerCase(Locale.ROOT);
        }
        return identifier.replaceAll("[^0-9+]", "");
    }
}
