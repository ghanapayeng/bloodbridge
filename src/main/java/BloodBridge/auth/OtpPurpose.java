package BloodBridge.auth;

import java.util.Locale;

/**
 * Defines the purpose of an OTP challenge to strictly isolate registration and login workflows.
 */
public enum OtpPurpose {
    REGISTRATION,
    LOGIN;

    public static String normalize(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return REGISTRATION.name();
        }
        String clean = purpose.trim().toUpperCase(Locale.ROOT);
        if ("LOGIN".equals(clean)) {
            return LOGIN.name();
        }
        return REGISTRATION.name();
    }
}
