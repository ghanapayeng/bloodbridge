package BloodBridge.notification;

/**
 * Service interface for dispatching transactional notification emails.
 */
public interface EmailService {

    /**
     * Sends an OTP verification email with default 5-minute expiration.
     *
     * @param recipientEmail Recipient's destination email
     * @param otpCode        The 6-digit one-time password
     */
    void sendOtpEmail(String recipientEmail, String otpCode);

    /**
     * Sends an OTP verification email with custom expiration in minutes.
     *
     * @param recipientEmail Recipient's destination email
     * @param otpCode        The 6-digit one-time password
     * @param expiryMinutes  Lifetime of the code in minutes
     */
    void sendOtpEmail(String recipientEmail, String otpCode, int expiryMinutes);
}
