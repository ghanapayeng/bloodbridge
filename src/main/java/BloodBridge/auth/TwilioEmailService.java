package BloodBridge.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class TwilioEmailService {

    private static final Logger log = LoggerFactory.getLogger(TwilioEmailService.class);

    private final boolean enabled;
    private final String apiUrl;
    private final String accountSid;
    private final String authToken;
    private final String fromAddress;
    private final String fromName;
    private final boolean trialFallback;

    private final HttpClient httpClient;

    public TwilioEmailService(
            @Value("${twilio.email.enabled:true}") boolean enabled,
            @Value("${twilio.email.api-url:https://comms.twilio.com/v1/Emails}") String apiUrl,
            @Value("${twilio.email.account-sid:}") String accountSid,
            @Value("${twilio.email.auth-token:}") String authToken,
            @Value("${twilio.email.from-address:}") String fromAddress,
            @Value("${twilio.email.from-name:BloodBridge}") String fromName,
            @Value("${twilio.email.trial-fallback:true}") boolean trialFallback) {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.accountSid = accountSid != null ? accountSid.trim() : "";
        this.authToken = authToken != null ? authToken.trim() : "";
        this.fromAddress = fromAddress != null ? fromAddress.trim() : "";
        this.fromName = fromName != null ? fromName.trim() : "BloodBridge";
        this.trialFallback = trialFallback;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends an OTP verification email to the specified recipient via Twilio Comms API.
     *
     * @param recipientEmail destination email address
     * @param otpCode 6-digit one-time password
     * @return true if email dispatch was accepted by Twilio or simulated
     */
    public boolean sendOtpEmail(String recipientEmail, String otpCode) {
        if (!enabled || accountSid.isBlank() || authToken.isBlank()) {
            log.info("[TWILIO EMAIL] Email dispatch skipped (credentials not configured in environment or disabled). Simulated OTP for {}: {}",
                    recipientEmail, otpCode);
            return true;
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[TWILIO EMAIL] Recipient email is blank. Skipping email send.");
            return false;
        }

        String subject = "Your BloodBridge Login Verification Code: " + otpCode;
        String htmlBody = buildOtpHtmlBody(otpCode);

        // Attempt 1: Standard branded BloodBridge OTP email
        try {
            HttpResponse<String> response = executeSend(recipientEmail, subject, htmlBody);
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                log.info("[TWILIO EMAIL] Successfully sent OTP email to {} (status: {})", recipientEmail, status);
                return true;
            }

            String responseBody = response.body() != null ? response.body() : "";
            log.warn("[TWILIO EMAIL] Initial send to {} returned status {}: {}", recipientEmail, status, responseBody);

            // Attempt 2: If Twilio Trial template restrictions are detected, fall back to approved trial template
            if (trialFallback && status == 400 && responseBody.contains("approved template")) {
                log.info("[TWILIO EMAIL] Twilio Trial template restriction detected. Retrying with approved trial template for {}...", recipientEmail);
                HttpResponse<String> trialResponse = executeSend(
                        recipientEmail,
                        "Your Order Has Been Confirmed!",
                        buildTrialApprovedHtmlBody()
                );

                if (trialResponse.statusCode() >= 200 && trialResponse.statusCode() < 300) {
                    log.info("[TWILIO EMAIL] Approved Twilio trial template successfully accepted for delivery to {} (status: {}). OTP Code: {}",
                            recipientEmail, trialResponse.statusCode(), otpCode);
                    return true;
                } else {
                    log.error("[TWILIO EMAIL] Trial fallback send failed with status {}: {}",
                            trialResponse.statusCode(), trialResponse.body());
                }
            }
        } catch (Exception e) {
            log.error("[TWILIO EMAIL] Error communicating with Twilio Comms API for {}: {}", recipientEmail, e.getMessage(), e);
        }

        return false;
    }

    private HttpResponse<String> executeSend(String toEmail, String subject, String html) throws Exception {
        String senderAddress = !fromAddress.isBlank() ? fromAddress : (accountSid + "@twilio.email");
        String jsonPayload = "{\"from\": {\"address\": \"" + escapeJson(senderAddress) + "\", \"name\": \"" + escapeJson(fromName) + "\"}, " +
                "\"to\": [{\"address\": \"" + escapeJson(toEmail) + "\"}], " +
                "\"content\": {\"subject\": \"" + escapeJson(subject) + "\", \"html\": \"" + escapeJson(html) + "\"}}";

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String buildOtpHtmlBody(String otpCode) {
        return "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 540px; margin: 0 auto; padding: 28px; border: 1px solid #f1e5e5; border-radius: 14px; background: #ffffff;\">" +
                "<div style=\"text-align: center; margin-bottom: 24px;\">" +
                "<div style=\"font-size: 28px; font-weight: 800; color: #dc2626; letter-spacing: -0.5px;\">♥ Blood<span style=\"color: #1e293b;\">Bridge</span></div>" +
                "<p style=\"color: #64748b; font-size: 13px; margin-top: 4px;\">Connecting Donors, Saving Lives</p>" +
                "</div>" +
                "<div style=\"background: #fef2f2; border: 1px solid #fee2e2; border-radius: 10px; padding: 22px; text-align: center; margin-bottom: 24px;\">" +
                "<p style=\"color: #991b1b; font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; margin: 0 0 10px 0;\">Your Login / Verification OTP</p>" +
                "<div style=\"font-size: 38px; font-weight: 900; letter-spacing: 8px; color: #dc2626; margin: 8px 0 12px;\">" + otpCode + "</div>" +
                "<p style=\"color: #64748b; font-size: 12px; margin: 0;\">Valid for 5 minutes. Do not share this code with anyone.</p>" +
                "</div>" +
                "<p style=\"color: #475569; font-size: 14px; line-height: 1.6; margin: 0 0 16px 0;\">You requested a one-time verification code to sign in to BloodBridge. If you did not make this request, please safely ignore this email.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #f1e5e5; margin: 24px 0 16px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 11px; text-align: center; margin: 0;\">&copy; BloodBridge Emergency Blood Donation Network &bull; Secured with Twilio</p>" +
                "</div>";
    }

    private String buildTrialApprovedHtmlBody() {
        return "<p><b>This is a test email from Twilio.</b></p>" +
                "<h2>Thank you for your order!</h2>" +
                "<p>We are excited to let you know that your order has been confirmed and is being processed.</p>" +
                "<p>You will receive a shipping confirmation email once your items are on their way.</p>" +
                "<p>Order Number: #12345</p>" +
                "<p>Thank you for shopping with us!</p>" +
                "<p>Best regards,<br/>The Team</p>";
    }

    private String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < ' ') {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
