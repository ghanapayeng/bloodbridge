package BloodBridge.auth;

import BloodBridge.auth.AuthDtos.RegisterRequest;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Transactional
    public UserAccount register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        if (userAccountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email address.");
        }

        // 1. Email verification handling: preserve verified state from UI or verify incoming OTP
        if (otpService.isRegistrationVerified(email, "EMAIL")) {
            log.info("[AUTH SERVICE] Email {} already verified for registration; preserving verified state.", email);
        } else if (request.emailOtp() != null && !request.emailOtp().isBlank()) {
            otpService.verifyOtp(email, "EMAIL", request.emailOtp(), OtpPurpose.REGISTRATION.name());
        }

        // 2. Phone verification handling: preserve verified state or verify incoming OTP
        if (request.phone() != null && !request.phone().isBlank()) {
            if (otpService.isRegistrationVerified(request.phone(), "PHONE")) {
                log.info("[AUTH SERVICE] Phone {} already verified for registration.", request.phone());
            } else if (request.phoneOtp() != null && !request.phoneOtp().isBlank()) {
                otpService.verifyOtp(request.phone(), "PHONE", request.phoneOtp(), OtpPurpose.REGISTRATION.name());
            }
        }

        // 3. Create and persist user account
        UserAccount user = new UserAccount(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()));
        UserAccount savedUser = userAccountRepository.save(user);

        // 4. Consume registration verification challenges so they cannot be reused
        otpService.consumeRegistrationVerification(email, "EMAIL");
        if (request.phone() != null && !request.phone().isBlank()) {
            otpService.consumeRegistrationVerification(request.phone(), "PHONE");
        }

        log.info("[AUTH SERVICE] Successfully registered user account for {}", email);
        return savedUser;
    }

    @Transactional(readOnly = true)
    public UserAccount requireByEmail(String email) {
        return userAccountRepository.findByEmail(normaliseEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required."));
    }

    @Transactional
    public UserAccount loginWithOtp(String rawEmail, String code) {
        String email = normaliseEmail(rawEmail);

        // Ensure user exists before consuming OTP challenge
        UserAccount user = userAccountRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found with this email. Please sign up first."));

        // Strictly verify OTP with LOGIN purpose
        otpService.verifyOtp(email, "EMAIL", code, OtpPurpose.LOGIN.name());

        log.info("[AUTH SERVICE] Successful OTP login for user {}", email);
        return user;
    }

    public String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
