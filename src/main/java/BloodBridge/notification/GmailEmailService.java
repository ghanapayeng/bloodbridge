package BloodBridge.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Service for dispatching transactional OTP verification emails via Gmail SMTP using JavaMailSender.
 */
@Service
public class GmailEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(GmailEmailService.class);

    private final JavaMailSender mailSender;
    private final String username;
    private final String password;

    @Autowired
    public GmailEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        this.mailSender = mailSender;
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password.trim() : "";
    }

    /**
     * Sends an OTP verification email with default 5-minute expiration.
     */
    @Override
    public void sendOtpEmail(String recipientEmail, String otpCode) {
        sendOtpEmail(recipientEmail, otpCode, 5);
    }

    /**
     * Sends an OTP verification email with custom expiration in minutes.
     *
     * @param recipientEmail Recipient's destination email
     * @param otpCode        The 6-digit one-time password
     * @param expiryMinutes  Lifetime of the code in minutes
     */
    @Override
    public void sendOtpEmail(String recipientEmail, String otpCode, int expiryMinutes) {
        if (username.isEmpty() || password.isEmpty()) {
            log.error("[GMAIL SMTP] Email dispatch aborted: GMAIL_USERNAME or GMAIL_APP_PASSWORD is not configured.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Email verification service is unconfigured. Please configure GMAIL_USERNAME and GMAIL_APP_PASSWORD in environment.");
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient email address is required.");
        }

        if (otpCode == null || otpCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Verification code is empty.");
        }

        String maskedRecipient = maskEmail(recipientEmail);
        String subject = "Your BloodBridge Verification Code";
        String htmlBody = buildHtmlEmail(otpCode, expiryMinutes);
        String textBody = buildTextEmail(otpCode, expiryMinutes);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(username, "BloodBridge");
            helper.setTo(recipientEmail.trim());
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            mailSender.send(mimeMessage);

            log.info("[GMAIL SMTP] Verification email successfully dispatched to {}", maskedRecipient);

        } catch (MailAuthenticationException e) {
            log.error("[GMAIL SMTP] Authentication failed for sender {}: Check GMAIL_USERNAME and GMAIL_APP_PASSWORD",
                    maskEmail(username));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Gmail SMTP authentication failed. Please verify your GMAIL_USERNAME and 16-character App Password.");
        } catch (MailSendException e) {
            log.error("[GMAIL SMTP] Failed to send email to {}: {}", maskedRecipient, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to deliver verification email via Gmail SMTP.");
        } catch (MailException e) {
            log.error("[GMAIL SMTP] SMTP error dispatching email to {}: {}", maskedRecipient, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to deliver verification email due to a mail service error.");
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[GMAIL SMTP] Message preparation error for recipient {}: {}", maskedRecipient, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to construct verification email message.");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[GMAIL SMTP] Unexpected error delivering email to recipient {}", maskedRecipient, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred while sending the verification email.");
        }
    }

    private String buildHtmlEmail(String otpCode, int expiryMinutes) {
        String cleanCode = otpCode != null ? otpCode.trim() : "";
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>BloodBridge Verification Code</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; color: #1e293b;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color: #f8fafc; padding: 40px 15px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" style="max-width: 560px; background-color: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
                      <tr>
                        <td style="background: linear-gradient(135deg, #b91c1c 0%, #dc2626 100%); padding: 32px 24px; text-align: center;">
                          <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;">
                            🩸 BloodBridge
                          </h1>
                          <p style="margin: 6px 0 0 0; color: #fecaca; font-size: 14px; font-weight: 500;">
                            Emergency Blood Coordination & Donor Network
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 36px 32px;">
                          <h2 style="margin: 0 0 16px 0; color: #0f172a; font-size: 20px; font-weight: 700;">
                            Verify Your Account
                          </h2>
                          <p style="margin: 0 0 24px 0; color: #475569; font-size: 15px; line-height: 1.6;">
                            Use the one-time verification code below to authenticate your BloodBridge account. This code is confidential and should never be shared.
                          </p>
                          
                          <div style="background-color: #fef2f2; border: 2px dashed #f87171; border-radius: 10px; padding: 20px; text-align: center; margin: 28px 0;">
                            <div style="color: #991b1b; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px;">
                              Your One-Time Password
                            </div>
                            <div style="color: #b91c1c; font-size: 38px; font-weight: 800; letter-spacing: 8px; font-family: 'Courier New', Courier, monospace;">
                              {{OTP_CODE}}
                            </div>
                            <div style="color: #dc2626; font-size: 13px; font-weight: 600; margin-top: 8px;">
                              ⏱️ Expires in {{EXPIRY_MINUTES}} minutes
                            </div>
                          </div>

                          <div style="background-color: #f1f5f9; border-radius: 8px; padding: 14px 18px; margin-top: 24px;">
                            <p style="margin: 0; color: #64748b; font-size: 13px; line-height: 1.5;">
                              🔒 <strong>Security Tip:</strong> BloodBridge staff will never ask for your verification code. If you did not initiate this request, please disregard this email.
                            </p>
                          </div>
                        </td>
                      </tr>
                      <tr>
                        <td style="background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px 32px; text-align: center;">
                          <p style="margin: 0; color: #94a3b8; font-size: 12px; line-height: 1.5;">
                            &copy; BloodBridge Lifesaving Network. Real-Time Hospital Inventory & Emergency Matching.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """
            .replace("{{OTP_CODE}}", cleanCode)
            .replace("{{EXPIRY_MINUTES}}", String.valueOf(expiryMinutes));
    }

    private String buildTextEmail(String otpCode, int expiryMinutes) {
        return """
            🩸 BloodBridge - Verification Code

            Your one-time password is: {{OTP_CODE}}

            This code will expire in {{EXPIRY_MINUTES}} minutes.

            Security Notice: Never share this code with anyone. BloodBridge staff will never ask for your verification code.
            If you did not request this verification code, please disregard this email.

            ---
            BloodBridge Lifesaving Network
            """
            .replace("{{OTP_CODE}}", otpCode != null ? otpCode.trim() : "")
            .replace("{{EXPIRY_MINUTES}}", String.valueOf(expiryMinutes));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
