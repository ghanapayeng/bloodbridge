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

import BloodBridge.notification.EmailService;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int COOLDOWN_SECONDS = 60;
    private static final int MAX_HOURLY_REQUESTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpVerificationRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void sendOtp(String rawIdentifier, String type) {
        sendOtp(rawIdentifier, type, OtpPurpose.REGISTRATION.name());
    }

    @Transactional
    public void sendOtp(String rawIdentifier, String type, String rawPurpose) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String purpose = OtpPurpose.normalize(rawPurpose);

        // 1. Rate limiting: 60-second cooldown per identifier and purpose
        Optional<OtpVerification> latestOpt = otpRepository
                .findTopByIdentifierAndOtpTypeAndPurposeOrderByCreatedAtDesc(identifier, type, purpose);
        if (latestOpt.isPresent()) {
            OtpVerification latest = latestOpt.get();
            long secondsSinceLast = ChronoUnit.SECONDS.between(latest.getCreatedAt(), Instant.now());
            if (secondsSinceLast < COOLDOWN_SECONDS) {
                long waitTime = COOLDOWN_SECONDS - secondsSinceLast;
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + Math.max(1, waitTime) + " seconds before requesting another code.");
            }
        }

        // 2. Rate limiting: max requests per hour per identifier and purpose
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long hourlyCount = otpRepository.countByIdentifierAndOtpTypeAndPurposeAndCreatedAtAfter(identifier, type, purpose, oneHourAgo);
        if (hourlyCount >= MAX_HOURLY_REQUESTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification requests. Please try again after an hour.");
        }

        // 3. Generate 6-digit cryptographic code
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);

        // 4. Store one-way cryptographic SHA-256 hash of the code
        String hashedCode = hashOtp(identifier, purpose, code);
        OtpVerification verification = new OtpVerification(identifier, hashedCode, type, purpose, expiresAt);
        otpRepository.save(verification);

        // 5. Dispatch email via Gmail SMTP without consuming or marking OTP as verified
        if ("EMAIL".equalsIgnoreCase(type)) {
            emailService.sendOtpEmail(identifier, code, 5);
        }

        log.info("[OTP SERVICE] Verification code generated and dispatched for {} ({}, purpose: {})",
                identifier, type, purpose);
    }

    @Transactional
    public boolean verifyOtp(String rawIdentifier, String type, String code) {
        return verifyOtp(rawIdentifier, type, code, OtpPurpose.REGISTRATION.name());
    }

    @Transactional
    public boolean verifyOtp(String rawIdentifier, String type, String code, String rawPurpose) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String purpose = OtpPurpose.normalize(rawPurpose);

        // Retrieve latest challenge for this identifier, type, and purpose with row lock for atomicity
        OtpVerification verification = otpRepository
                .findFirstByIdentifierAndOtpTypeAndPurposeOrderByCreatedAtDesc(identifier, type, purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No " + purpose.toLowerCase(Locale.ROOT) + " verification code was requested for this identifier."));

        // Single-use check: already verified codes cannot be reused
        if (verification.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verification code has already been used. Please request a new one.");
        }

        // Expiration check (5 minutes)
        if (verification.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verification code has expired. Please request a new one.");
        }

        // Brute-force lockout prevention: max failed attempts check
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Maximum verification attempts exceeded. Please request a new code.");
        }

        // Secure cryptographic hash matching
        if (!matchesOtp(identifier, purpose, code != null ? code.trim() : "", verification.getOtpCode())) {
            int failed = verification.getFailedAttempts() + 1;
            verification.setFailedAttempts(failed);
            otpRepository.save(verification);

            int remaining = MAX_FAILED_ATTEMPTS - failed;
            if (remaining > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid verification code entered. " + remaining + " attempts remaining.");
            } else {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Maximum verification attempts exceeded. Please request a new code.");
            }
        }

        // Mark verified atomically to guarantee concurrent requests cannot reuse the same OTP
        int updated = otpRepository.markVerifiedAtomically(verification.getId());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verification code has already been used. Please request a new one.");
        }

        verification.setVerified(true);
        log.info("[OTP SERVICE] Successfully verified code for {} ({}, purpose: {})", identifier, type, purpose);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isRegistrationVerified(String rawIdentifier, String type) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        return otpRepository
                .findTopByIdentifierAndOtpTypeAndPurposeOrderByCreatedAtDesc(identifier, type, OtpPurpose.REGISTRATION.name())
                .map(v -> v.isVerified() && !v.isExpired())
                .orElse(false);
    }

    @Transactional
    public void consumeRegistrationVerification(String rawIdentifier, String type) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        otpRepository.deleteByIdentifierAndOtpTypeAndPurpose(identifier, type, OtpPurpose.REGISTRATION.name());
    }

    @Transactional(readOnly = true)
    public boolean isVerified(String rawIdentifier, String type) {
        return isVerified(rawIdentifier, type, OtpPurpose.REGISTRATION.name());
    }

    @Transactional(readOnly = true)
    public boolean isVerified(String rawIdentifier, String type, String rawPurpose) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String purpose = OtpPurpose.normalize(rawPurpose);
        return otpRepository
                .findTopByIdentifierAndOtpTypeAndPurposeOrderByCreatedAtDesc(identifier, type, purpose)
                .map(v -> v.isVerified() && !v.isExpired())
                .orElse(false);
    }

    public void createOtpForTesting(String rawIdentifier, String type, String purpose, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String normPurpose = OtpPurpose.normalize(purpose);
        String hashedCode = hashOtp(identifier, normPurpose, code);
        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        OtpVerification verification = new OtpVerification(identifier, hashedCode, type, normPurpose, expiresAt);
        otpRepository.save(verification);
    }

    public void createOtpForTesting(String rawIdentifier, String type, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        createOtpForTesting(identifier, type, OtpPurpose.LOGIN.name(), code);
        createOtpForTesting(identifier, type, OtpPurpose.REGISTRATION.name(), code);
    }

    public void createExpiredOtpForTesting(String rawIdentifier, String type, String purpose, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String normPurpose = OtpPurpose.normalize(purpose);
        String hashedCode = hashOtp(identifier, normPurpose, code);
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.MINUTES);
        OtpVerification verification = new OtpVerification(identifier, hashedCode, type, normPurpose, expiresAt);
        otpRepository.save(verification);
    }

    public void createExpiredOtpForTesting(String rawIdentifier, String type, String code) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        createExpiredOtpForTesting(identifier, type, OtpPurpose.REGISTRATION.name(), code);
        createExpiredOtpForTesting(identifier, type, OtpPurpose.LOGIN.name(), code);
    }

    private String hashOtp(String identifier, String purpose, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedInput = identifier + ":" + purpose + ":" + code;
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

    private String hashOtpLegacy(String identifier, String code) {
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

    private boolean matchesOtp(String identifier, String purpose, String rawCode, String storedOtp) {
        if (storedOtp == null || rawCode == null || rawCode.isBlank()) {
            return false;
        }
        // Legacy plaintext compatibility check
        if (storedOtp.length() == 6 && storedOtp.equals(rawCode)) {
            return true;
        }
        // Hash with purpose (primary)
        String hashedWithPurpose = hashOtp(identifier, purpose, rawCode);
        if (MessageDigest.isEqual(
                hashedWithPurpose.getBytes(StandardCharsets.UTF_8),
                storedOtp.getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        // Fallback for legacy hashes without purpose in salt
        String hashedLegacy = hashOtpLegacy(identifier, rawCode);
        return MessageDigest.isEqual(
                hashedLegacy.getBytes(StandardCharsets.UTF_8),
                storedOtp.getBytes(StandardCharsets.UTF_8));
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
