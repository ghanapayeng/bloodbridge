package BloodBridge.auth;

import BloodBridge.auth.AuthDtos.LoginRequest;
import BloodBridge.auth.AuthDtos.OtpLoginRequest;
import BloodBridge.auth.AuthDtos.OtpResponse;
import BloodBridge.auth.AuthDtos.RegisterRequest;
import BloodBridge.auth.AuthDtos.SendOtpRequest;
import BloodBridge.auth.AuthDtos.UserResponse;
import BloodBridge.auth.AuthDtos.VerifyOtpRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            OtpService otpService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.otpService = otpService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<OtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String purpose = request.purpose();
        if (purpose == null || purpose.isBlank()) {
            boolean userExists = authService.userExists(request.identifier());
            purpose = userExists ? OtpPurpose.LOGIN.name() : OtpPurpose.REGISTRATION.name();
        }
        otpService.sendOtp(request.identifier(), request.type(), purpose);
        return ResponseEntity.ok(new OtpResponse(true, "A verification code has been sent to your email."));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<OtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String purpose = request.purpose() != null && !request.purpose().isBlank() ? request.purpose() : OtpPurpose.REGISTRATION.name();
        boolean valid = otpService.verifyOtp(request.identifier(), request.type(), request.code(), purpose);
        return ResponseEntity.ok(new OtpResponse(valid, "Verification successful."));
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        authService.normaliseEmail(request.email()), request.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        return UserResponse.from(authService.requireByEmail(authentication.getName()));
    }

    @PostMapping("/login/otp")
    public UserResponse loginWithOtp(
            @Valid @RequestBody OtpLoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        UserAccount user = authService.loginWithOtp(request.email(), request.code());

        String roleName = user.getRole() != null ? user.getRole() : "ROLE_USER";
        if (roleName.startsWith("ROLE_")) {
            roleName = roleName.substring(5);
        }

        UserDetails userDetails = User
                .withUsername(user.getEmail())
                .password("")
                .roles(roleName)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);

        return UserResponse.from(user);
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(CsrfToken token, HttpServletRequest request) {
        if (token == null) {
            String sessionToken = request.getSession(true).getId();
            return new CsrfTokenResponse("X-XSRF-TOKEN", "_csrf", sessionToken);
        }
        return new CsrfTokenResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return UserResponse.from(authService.requireByEmail(authentication.getName()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    public record CsrfTokenResponse(String headerName, String parameterName, String token) {
    }
}
