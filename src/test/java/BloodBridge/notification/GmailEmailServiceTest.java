package BloodBridge.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

class GmailEmailServiceTest {

    private static final String TEST_USERNAME = "bloodbridge.notifications@gmail.com";
    private static final String TEST_PASSWORD = "abcd efgh ijkl mnop";

    private JavaMailSenderImpl mailSender;
    private GmailEmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = spy(new JavaMailSenderImpl());
        emailService = new GmailEmailService(mailSender, TEST_USERNAME, TEST_PASSWORD);
    }

    @Test
    void testSuccessfulEmailDispatchAndMimeMessageConstruction() throws Exception {
        AtomicReference<MimeMessage> capturedMessage = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedMessage.set(invocation.getArgument(0));
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        emailService.sendOtpEmail("donor.test@example.com", "849201", 5);

        MimeMessage message = capturedMessage.get();
        assertNotNull(message, "MimeMessage was not dispatched to JavaMailSender");

        // Verify recipient
        Address[] recipients = message.getAllRecipients();
        assertNotNull(recipients);
        assertEquals(1, recipients.length);
        assertEquals("donor.test@example.com", recipients[0].toString());

        // Verify sender
        Address[] from = message.getFrom();
        assertNotNull(from);
        assertEquals(1, from.length);
        assertTrue(from[0].toString().contains(TEST_USERNAME));
        assertTrue(from[0].toString().contains("BloodBridge"));

        // Verify subject
        assertEquals("Your BloodBridge Verification Code", message.getSubject());

        // Verify content includes OTP code and expiration
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        String rawEmail = baos.toString();

        assertTrue(rawEmail.contains("849201"), "Email content must contain OTP code");
        assertTrue(rawEmail.contains("5 minutes"), "Email content must state 5-minute expiry");
        assertTrue(rawEmail.contains("BloodBridge"), "Email content must include BloodBridge branding");
    }

    @Test
    void testDefaultExpiryOverloadDispatchesSuccessfully() throws Exception {
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendOtpEmail("user@example.com", "123456");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void testMissingUsernameThrowsInternalServerError() {
        GmailEmailService service = new GmailEmailService(mailSender, "", TEST_PASSWORD);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.sendOtpEmail("test@example.com", "123456"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("GMAIL_USERNAME"));
    }

    @Test
    void testMissingPasswordThrowsInternalServerError() {
        GmailEmailService service = new GmailEmailService(mailSender, TEST_USERNAME, "   ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.sendOtpEmail("test@example.com", "123456"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("GMAIL_APP_PASSWORD"));
    }

    @Test
    void testBlankRecipientThrowsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.sendOtpEmail("   ", "123456"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Recipient email address is required"));
    }

    @Test
    void testBlankOtpCodeThrowsInternalServerError() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.sendOtpEmail("user@example.com", "  "));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Verification code is empty"));
    }

    @Test
    void testMailAuthenticationExceptionThrowsInternalServerErrorWithoutLeakingPassword() {
        doThrow(new MailAuthenticationException("Authentication failed for user"))
                .when(mailSender).send(any(MimeMessage.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.sendOtpEmail("user@example.com", "654321"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertFalse(ex.getReason().contains(TEST_PASSWORD), "App password must never leak in error response");
        assertTrue(ex.getReason().contains("authentication failed"));
        assertTrue(ex.getReason().contains("GMAIL_USERNAME"));
    }

    @Test
    void testMailSendExceptionThrowsBadGateway() {
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.sendOtpEmail("user@example.com", "654321"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Failed to deliver verification email"));
    }

    @Test
    void testResendEmailServiceBackwardCompatibilitySubclass() throws Exception {
        ResendEmailService legacyService = new ResendEmailService(mailSender, TEST_USERNAME, TEST_PASSWORD);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        legacyService.sendOtpEmail("legacy@example.com", "999888", 5);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
