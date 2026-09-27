package BloodBridge.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "otp_verifications")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String identifier;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "otp_type", nullable = false, length = 32)
    private String otpType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpVerification() {
    }

    public OtpVerification(String identifier, String otpCode, String otpType, Instant expiresAt) {
        this.identifier = identifier;
        this.otpCode = otpCode;
        this.otpType = otpType;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public String getOtpType() {
        return otpType;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
