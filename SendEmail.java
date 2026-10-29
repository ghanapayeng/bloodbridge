import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SendEmail {

    public static void main(String[] args) throws Exception {
        Map<String, String> env = loadDotEnv();

        String apiKeySid = getVar("TWILIO_API_KEY_SID", env);
        if (apiKeySid == null || apiKeySid.isBlank()) {
            apiKeySid = getVar("TWILIO_ACCOUNT_SID", env);
        }

        String apiKeySecret = getVar("TWILIO_API_KEY_SECRET", env);
        if (apiKeySecret == null || apiKeySecret.isBlank()) {
            apiKeySecret = getVar("TWILIO_AUTH_TOKEN", env);
        }

        String apiUrl = getVar("TWILIO_EMAIL_API_URL", env);
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://comms.twilio.com/v1/Emails";
        }

        String fromAddress = getVar("TWILIO_FROM_ADDRESS", env);
        String fromName = getVar("TWILIO_FROM_NAME", env);
        if (fromName == null || fromName.isBlank()) {
            fromName = "BloodBridge";
        }

        if (apiKeySid == null || apiKeySid.isBlank() || apiKeySecret == null || apiKeySecret.isBlank()) {
            System.err.println("❌ Error: Missing credentials. Please set TWILIO_API_KEY_SID and TWILIO_API_KEY_SECRET in your environment or .env file.");
            System.exit(1);
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            System.err.println("❌ Error: Missing sender address. Please set TWILIO_FROM_ADDRESS in your environment or .env file.");
            System.exit(1);
        }

        String recipient = (args.length > 0 && !args[0].isBlank()) ? args[0].trim() : "test@example.com";

        String credentials = Base64.getEncoder().encodeToString(
                (apiKeySid + ":" + apiKeySecret).getBytes(StandardCharsets.UTF_8));

        String testSubject = "BloodBridge Email Service Connectivity Test";
        String testHtml = """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background: #ffffff;">
              <h2 style="color: #dc2626; margin: 0 0 12px 0;">BloodBridge</h2>
              <p style="color: #334155; font-size: 15px; margin: 0 0 12px 0;">This is an automated connectivity test for the BloodBridge email dispatch service.</p>
              <p style="color: #16a34a; font-weight: 600; font-size: 14px; margin: 0 0 16px 0;">✓ Twilio Comms Email API connectivity verified.</p>
              <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 0 0 16px 0;" />
              <p style="color: #64748b; font-size: 12px; margin: 0;">Sent via Twilio Comms API</p>
            </div>""";

        String json = """
            {
              "from": {"address": "%s", "name": "%s"},
              "to": [{"address": "%s"}],
              "content": {"subject": "%s", "html": "%s"}
            }""".formatted(
                escapeJson(fromAddress),
                escapeJson(fromName),
                escapeJson(recipient),
                escapeJson(testSubject),
                escapeJson(testHtml)
        );

        System.out.println("==================================================");
        System.out.println("Twilio Comms Email Connectivity Test");
        System.out.println("==================================================");
        System.out.println("• Endpoint: " + apiUrl);
        System.out.println("• From:     " + fromAddress + " (" + fromName + ")");
        System.out.println("• To:       " + maskEmail(recipient));
        System.out.println("• Key SID:  " + maskKey(apiKeySid));
        System.out.println("--------------------------------------------------");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + credentials)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        int status = response.statusCode();
        System.out.println("HTTP Status: " + status);
        System.out.println("Response:    " + response.body());
        System.out.println("--------------------------------------------------");

        if (status >= 200 && status < 300) {
            System.out.println("✅ Email dispatched successfully!");
        } else {
            System.err.println("❌ Email dispatch failed with HTTP " + status);
        }
    }

    private static String maskKey(String key) {
        if (key == null || key.length() < 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String maskedName = name.length() <= 2 ? "**" : name.charAt(0) + "***" + name.charAt(name.length() - 1);
        return maskedName + "@" + parts[1];
    }

    private static String getVar(String key, Map<String, String> fileEnv) {
        String sysEnv = System.getenv(key);
        if (sysEnv != null && !sysEnv.isBlank()) {
            return sysEnv.trim();
        }
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String val = fileEnv.get(key);
        return val != null ? val.trim() : null;
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> map = new HashMap<>();
        File file = new File(".env");
        if (!file.exists()) {
            return map;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if ((v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) ||
                    (v.startsWith("'") && v.endsWith("'") && v.length() >= 2)) {
                    v = v.substring(1, v.length() - 1);
                }
                map.put(k, v);
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
