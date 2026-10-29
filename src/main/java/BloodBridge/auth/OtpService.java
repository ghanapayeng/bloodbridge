package BloodBridge.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

import BloodBridge.notification.ResendEmailService;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_HOURLY_REQUESTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final ResendEmailService resendEmailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpVerificationRepository otpRepository, ResendEmailService resendEmailService) {
        this.otpRepository = otpRepository;
        this.resendEmailService = resendEmailService;
    }

    @Transactional
    public void sendOtp(String rawIdentifier, String type) {
        String identifier = normalizeIdentifier(rawIdentifier, type);

        // 1. Rate limiting: 60-second cooldown
        Optional<OtpVerification> latestOpt = otpRepository.findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(identifier, type);
        if (latestOpt.isPresent()) {
            OtpVerification latest = latestOpt.get();
            long secondsSinceLast = ChronoUnit.SECONDS.between(latest.getCreatedAt(), Instant.now());
            if (secondsSinceLast < COOLDOWN_SECONDS) {
                long waitTime = COOLDOWN_SECONDS - secondsSinceLast;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + Math.max(1, waitTime) + " seconds before requesting another code.");
            }
        }

        // 2. Rate limiting: max requests per hour
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long hourlyCount = otpRepository.countByIdentifierAndOtpTypeAndCreatedAtAfter(identifier, type, oneHourAgo);
        if (hourlyCount >= MAX_HOURLY_REQUESTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification requests. Please try again after an hour.");
        }

        // 3. Generate 6-digit cryptographic code
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        // 4. Store one-way cryptographic hash of the code
        String hashedCode = hashOtp(identifier, code);
        OtpVerification verification = new OtpVerification(identifier, hashedCode, type, expiresAt);
        otpRepository.save(verification);

        // 5. Dispatch email via Resend API
        if ("EMAIL".equalsIgnoreCase(type)) {
            resendEmailService.sendOtpEmail(identifier, code, 5);
        }

        log.info("[OTP SERVICE] Verification code generated and dispatched for {} ({})", identifier, type);
    }

    @Transactional
    public boolean verifyOtp(String rawIdentifier, String type, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        OtpVerification verification = otpRepository
                .findTopByIdentifierAndOtpTypeOrderByCreatedAtDesc(identifier, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No verification code was requested for this identifier."));

        // Single use check
        if (verification.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has already been used. Please request a new one.");
        }

        // Expiration check (5 minutes)
        if (verification.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has expired. Please request a new one.");
        }

        // Brute force prevention: max attempts check
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum verification attempts exceeded. Please request a new code.");
        }

        // Secure hash matching
        if (!matchesOtp(identifier, code != null ? code.trim() : "", verification.getOtpCode())) {
            int failed = verification.getFailedAttempts() + 1;
            verification.setFailedAttempts(failed);
            otpRepository.save(verification);

            int remaining = MAX_FAILED_ATTEMPTS - failed;
            if (remaining > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code entered. " + remaining + " attempts remaining.");
            } else {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Maximum verification attempts exceeded. Please request a new code.");
            }
        }

        // Mark verified (single-use enforced)
        verification.setVerified(true);
        otpRepository.save(verification);
        log.info("[OTP SERVICE] Successfully verified code for {} ({})", identifier, type);
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

    public void createOtpForTesting(String rawIdentifier, String type, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String hashedCode = hashOtp(identifier, code);
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        OtpVerification verification = new OtpVerification(identifier, hashedCode, type, expiresAt);
        otpRepository.save(verification);
    }

    public void createExpiredOtpForTesting(String rawIdentifier, String type, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String hashedCode = hashOtp(identifier, code);
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES);
        OtpVerification verification = new OtpVerification(identifier, hashedCode, type, expiresAt);
        otpRepository.save(verification);
    }

    private String hashOtp(String identifier, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedInput = identifier + ":" + code;
            byte[] hash = digest.digest(saltedInput.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private boolean matchesOtp(String identifier, String rawCode, String storedOtp) {
        if (storedOtp == null || rawCode == null || rawCode.isBlank()) {
            return false;
        }
        // Legacy plaintext compatibility check
        if (storedOtp.length() == 6 && storedOtp.equals(rawCode)) {
            return true;
        }
        String hashedInput = hashOtp(identifier, rawCode);
        return MessageDigest.isEqual(
                hashedInput.getBytes(StandardCharsets.UTF_8),
                storedOtp.getBytes(StandardCharsets.UTF_8)
        );
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
