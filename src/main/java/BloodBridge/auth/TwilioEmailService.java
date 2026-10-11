package BloodBridge.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private final String apiKeySid;
    private final String apiKeySecret;
    private final String fromAddress;
    private final String fromName;

    private final HttpClient httpClient;

    public TwilioEmailService(
            @Value("${twilio.email.enabled:true}") boolean enabled,
            @Value("${twilio.email.api-url:https://comms.twilio.com/v1/Emails}") String apiUrl,
            @Value("${twilio.email.api-key-sid:}") String apiKeySid,
            @Value("${twilio.email.api-key-secret:}") String apiKeySecret,
            @Value("${twilio.email.from-address:}") String fromAddress,
            @Value("${twilio.email.from-name:BloodBridge}") String fromName) {
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.apiKeySid = apiKeySid != null ? apiKeySid.trim() : "";
        this.apiKeySecret = apiKeySecret != null ? apiKeySecret.trim() : "";
        this.fromAddress = fromAddress != null ? fromAddress.trim() : "";
        this.fromName = (fromName != null && !fromName.isBlank()) ? fromName.trim() : "BloodBridge";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends an OTP verification email to the specified recipient via Twilio Comms API.
     *
     * @param recipientEmail destination email address
     * @param otpCode 6-digit one-time password
     * @return true if email dispatch was accepted by Twilio
     */
    public boolean sendOtpEmail(String recipientEmail, String otpCode) {
        if (!enabled) {
            log.info("[TWILIO EMAIL] Email dispatch is disabled by configuration (twilio.email.enabled=false).");
            return true;
        }

        if (apiKeySid.isBlank() || apiKeySecret.isBlank()) {
            log.error("[TWILIO EMAIL] Twilio API Key credentials are missing. twilio.email.api-key-sid and twilio.email.api-key-secret must be configured.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is temporarily unconfigured. Please contact support.");
        }

        if (fromAddress.isBlank()) {
            log.error("[TWILIO EMAIL] Sender address is missing. twilio.email.from-address must be configured.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email service sender is unconfigured. Please contact support.");
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[TWILIO EMAIL] Recipient email is blank. Skipping email send.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient email address is required.");
        }

        String subject = "BloodBridge Verification Code: " + otpCode;
        String htmlBody = buildOtpHtmlBody(otpCode);

        try {
            HttpResponse<String> response = executeSend(fromAddress, recipientEmail, subject, htmlBody);
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                log.info("[TWILIO EMAIL] Verification email dispatched successfully to recipient (HTTP {})", status);
                return true;
            }

            String responseBody = response.body() != null ? response.body() : "";
            log.error("[TWILIO EMAIL] Failed to send OTP email. Twilio returned HTTP {}: {}", status, responseBody);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to deliver verification email. Please try again later.");

        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("[TWILIO EMAIL] Connection error communicating with Twilio Comms API: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to reach email delivery service. Please try again later.");
        }
    }

    private HttpResponse<String> executeSend(String sender, String toEmail, String subject, String html) throws Exception {
        String jsonPayload = "{\"from\": {\"address\": \"" + escapeJson(sender) + "\", \"name\": \"" + escapeJson(fromName) + "\"}, " +
                "\"to\": [{\"address\": \"" + escapeJson(toEmail) + "\"}], " +
                "\"content\": {" +
                "\"subject\": \"" + escapeJson(subject) + "\", " +
                "\"html\": \"" + escapeJson(html) + "\"" +
                "}}";

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (apiKeySid + ":" + apiKeySecret).getBytes(StandardCharsets.UTF_8));

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
        return "<div style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 28px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;\">" +
                "<h2 style=\"color: #dc2626; margin: 0 0 16px 0;\">BloodBridge</h2>" +
                "<p style=\"color: #334155; font-size: 15px; margin: 0 0 16px 0;\">Hello,</p>" +
                "<p style=\"color: #334155; font-size: 15px; margin: 0 0 12px 0;\">Your BloodBridge verification code is:</p>" +
                "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; text-align: center; margin: 0 0 16px 0;\">" +
                "<span style=\"font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #dc2626;\">" + otpCode + "</span>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 13px; margin: 0 0 12px 0;\">This code will expire in 5 minutes.</p>" +
                "<p style=\"color: #64748b; font-size: 13px; margin: 0 0 24px 0;\">If you did not request this code, please ignore this email.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #f1f5f9; margin: 0 0 16px 0;\" />" +
                "<p style=\"color: #475569; font-size: 13px; margin: 0;\">Regards,<br/><strong>BloodBridge Team</strong></p>" +
                "</div>";
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
