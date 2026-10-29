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
import BloodBridge.auth.AuthDtos.VerifyOtpRequest;
import BloodBridge.notification.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Transactional
@Import(OtpAuthIntegrationTest.MockEmailTestConfig.class)
class OtpAuthIntegrationTest {

    @TestConfiguration
    static class MockEmailTestConfig {

        public static final List<String> sentRecipients = new ArrayList<>();
        public static final List<String> sentCodes = new ArrayList<>();

        @Bean
        @Primary
        public EmailService testEmailService() {
            return new EmailService() {
                @Override
                public void sendOtpEmail(String recipientEmail, String otpCode) {
                    sendOtpEmail(recipientEmail, otpCode, 5);
                }

                @Override
                public void sendOtpEmail(String recipientEmail, String otpCode, int expiryMinutes) {
                    sentRecipients.add(recipientEmail);
                    sentCodes.add(otpCode);
                }
            };
        }
    }

    @Autowired
    private AuthController authController;

    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        MockEmailTestConfig.sentRecipients.clear();
        MockEmailTestConfig.sentCodes.clear();
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
        assertTrue(ex.getReason().contains("already been used"));
    }

    @Test
    void testExpiredOtpFailsVerification() {
        String email = "expired@example.com";
        otpService.createExpiredOtpForTesting(email, "EMAIL", "998877");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            otpService.verifyOtp(email, "EMAIL", "998877");
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("expired"));
    }

    @Test
    void testOtpSendRateLimitingCooldown() {
        String email = "cooldown@example.com";
        otpService.sendOtp(email, "EMAIL");

        // Verify email service was invoked
        assertTrue(MockEmailTestConfig.sentRecipients.contains(email));

        // Rapid second request within 60s should be blocked
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            otpService.sendOtp(email, "EMAIL");
        });
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Please wait"));
    }

    @Test
    void testOtpHourlyRateLimiting() {
        String email = "hourlylimit@example.com";

        // Seed 5 verifications in the last hour (> 60s ago to bypass cooldown)
        for (int i = 0; i < 5; i++) {
            Instant createdAt = Instant.now().minus(2 + i, ChronoUnit.MINUTES);
            OtpVerification v = new OtpVerification(email, "hash" + i, "EMAIL", Instant.now().plus(5, ChronoUnit.MINUTES), createdAt);
            otpRepository.save(v);
        }

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            otpService.sendOtp(email, "EMAIL");
        });
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Too many verification requests"));
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
    void testAuthControllerSendOtpDoesNotExposeCode() {
        ResponseEntity<OtpResponse> entity = authController.sendOtp(new SendOtpRequest("safe@example.com", "EMAIL"));
        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        OtpResponse response = entity.getBody();
        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("A verification code has been sent to your email.", response.message());

        // Verify email was dispatched via email service mock
        assertTrue(MockEmailTestConfig.sentRecipients.contains("safe@example.com"));
    }

    @Test
    void testAuthControllerVerifyOtpEndpoint() {
        String email = "verify-endpoint@example.com";
        otpService.createOtpForTesting(email, "EMAIL", "445566");

        ResponseEntity<OtpResponse> entity = authController.verifyOtp(new VerifyOtpRequest(email, "EMAIL", "445566"));
        assertNotNull(entity);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertTrue(entity.getBody().success());
        assertEquals("Verification successful.", entity.getBody().message());
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
