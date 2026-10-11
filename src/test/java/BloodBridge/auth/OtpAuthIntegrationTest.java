package BloodBridge.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import BloodBridge.auth.AuthDtos.OtpLoginRequest;
import BloodBridge.auth.AuthDtos.OtpResponse;
import BloodBridge.auth.AuthDtos.RegisterRequest;
import BloodBridge.auth.AuthDtos.SendOtpRequest;
import BloodBridge.auth.AuthDtos.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@Transactional
class OtpAuthIntegrationTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TwilioEmailService twilioEmailService;

    @BeforeEach
    void setUp() {
        authService.register(new RegisterRequest("Existing User", "existing@example.com", "Password123!456"));
    }

    @Test
    void testExistingUserOtpLogin() {
        String email = "existing@example.com";
        otpService.createOtpForTesting(email, "EMAIL", "123456");

        UserAccount user = authService.loginWithOtp(email, "123456");
        assertNotNull(user);
        assertEquals("existing@example.com", user.getEmail());
        assertEquals("Existing User", user.getFullName());
    }

    @Test
    void testNewUserOtpLoginFailsWithNotFound() {
        String newEmail = "unregistered@example.com";
        otpService.createOtpForTesting(newEmail, "EMAIL", "123456");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            authService.loginWithOtp(newEmail, "123456");
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No account found with this email"));
    }

    @Test
    void testInvalidOtpLoginFails() {
        String email = "existing@example.com";
        otpService.createOtpForTesting(email, "EMAIL", "123456");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            authService.loginWithOtp(email, "000000");
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testSingleUseOtpCannotBeReused() {
        String email = "existing@example.com";
        otpService.createOtpForTesting(email, "EMAIL", "654321");

        // First verification should succeed
        otpService.verifyOtp(email, "EMAIL", "654321");

        // Second verification with the same OTP must fail
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            otpService.verifyOtp(email, "EMAIL", "654321");
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testOtpSendRateLimitingCooldown() {
        String email = "cooldown@example.com";
        otpService.sendOtp(email, "EMAIL");

        // Rapid second request within 60s should be blocked
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            otpService.sendOtp(email, "EMAIL");
        });
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Please wait"));
    }

    @Test
    void testOtpFailedAttemptsLockout() {
        String email = "lockout@example.com";
        otpService.createOtpForTesting(email, "EMAIL", "777888");

        // 5 wrong attempts
        for (int i = 0; i < 5; i++) {
            assertThrows(ResponseStatusException.class, () -> {
                otpService.verifyOtp(email, "EMAIL", "000000");
            });
        }

        // 6th attempt (even with the correct code) should be locked out
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            otpService.verifyOtp(email, "EMAIL", "777888");
        });
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Maximum verification attempts exceeded"));
    }

    @Test
    void testTwilioEmailServiceDisabledMode() {
        boolean sent = twilioEmailService.sendOtpEmail("test@example.com", "123456");
        assertTrue(sent);
    }

    @Test
    void testAuthControllerSendOtpDoesNotExposeCode() {
        ResponseEntity<OtpResponse> entity = authController.sendOtp(new SendOtpRequest("safe@example.com", "EMAIL"));
        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        OtpResponse response = entity.getBody();
        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("A verification code has been sent to your email.", response.message());
    }

    @Test
    void testAuthControllerLoginWithOtp() {
        String email = "existing@example.com";
        otpService.createOtpForTesting(email, "EMAIL", "112233");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserResponse userResponse = authController.loginWithOtp(
                new OtpLoginRequest(email, "112233"),
                request,
                response
        );

        assertNotNull(userResponse);
        assertEquals("existing@example.com", userResponse.email());
        assertEquals("Existing User", userResponse.fullName());
    }
}
