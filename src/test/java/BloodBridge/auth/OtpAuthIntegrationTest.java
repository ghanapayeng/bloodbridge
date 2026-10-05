package BloodBridge.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import BloodBridge.auth.AuthDtos.OtpLoginRequest;
import BloodBridge.auth.AuthDtos.RegisterRequest;
import BloodBridge.auth.AuthDtos.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
        String otpCode = otpService.sendOtp(email, "EMAIL");
        assertNotNull(otpCode);
        assertEquals(6, otpCode.length());

        UserAccount user = authService.loginWithOtp(email, otpCode);
        assertNotNull(user);
        assertEquals("existing@example.com", user.getEmail());
        assertEquals("Existing User", user.getFullName());
    }

    @Test
    void testNewUserOtpAutoRegistrationAndLogin() {
        String newEmail = "newdonor@example.com";
        String otpCode = otpService.sendOtp(newEmail, "EMAIL");
        assertNotNull(otpCode);

        UserAccount user = authService.loginWithOtp(newEmail, otpCode);
        assertNotNull(user);
        assertEquals("newdonor@example.com", user.getEmail());
        assertTrue(userAccountRepository.existsByEmail("newdonor@example.com"));
    }

    @Test
    void testInvalidOtpLoginFails() {
        String email = "existing@example.com";
        otpService.sendOtp(email, "EMAIL");

        assertThrows(ResponseStatusException.class, () -> {
            authService.loginWithOtp(email, "000000");
        });
    }

    @Test
    void testTwilioEmailServiceDisabledMode() {
        boolean sent = twilioEmailService.sendOtpEmail("test@example.com", "123456");
        assertTrue(sent);
    }

    @Test
    void testAuthControllerLoginWithOtp() {
        String email = "existing@example.com";
        String otpCode = otpService.sendOtp(email, "EMAIL");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserResponse userResponse = authController.loginWithOtp(
                new OtpLoginRequest(email, otpCode),
                request,
                response
        );

        assertNotNull(userResponse);
        assertEquals("existing@example.com", userResponse.email());
        assertEquals("Existing User", userResponse.fullName());
    }
}
