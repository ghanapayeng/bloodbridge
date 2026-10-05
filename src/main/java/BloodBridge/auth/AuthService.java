package BloodBridge.auth;

import BloodBridge.auth.AuthDtos.RegisterRequest;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

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

        if (request.emailOtp() != null && !request.emailOtp().isBlank()) {
            otpService.verifyOtp(email, "EMAIL", request.emailOtp());
        }

        if (request.phone() != null && !request.phone().isBlank() && request.phoneOtp() != null && !request.phoneOtp().isBlank()) {
            otpService.verifyOtp(request.phone(), "PHONE", request.phoneOtp());
        }

        UserAccount user = new UserAccount(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()));
        return userAccountRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserAccount requireByEmail(String email) {
        return userAccountRepository.findByEmail(normaliseEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required."));
    }

    @Transactional
    public UserAccount loginWithOtp(String rawEmail, String code) {
        String email = normaliseEmail(rawEmail);
        otpService.verifyOtp(email, "EMAIL", code);

        return userAccountRepository.findByEmail(email).orElseGet(() -> {
            String namePart = email.split("@")[0];
            String displayName = (namePart != null && !namePart.isBlank())
                    ? Character.toUpperCase(namePart.charAt(0)) + namePart.substring(1)
                    : "Donor";

            UserAccount newUser = new UserAccount(
                    displayName,
                    email,
                    passwordEncoder.encode(java.util.UUID.randomUUID().toString()),
                    "ROLE_USER"
            );
            return userAccountRepository.save(newUser);
        });
    }

    public String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
