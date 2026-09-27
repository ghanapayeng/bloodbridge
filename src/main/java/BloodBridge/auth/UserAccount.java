package BloodBridge.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 32)
    private String role = "ROLE_USER";

    @Column(name = "donor_uuid", nullable = false, unique = true, length = 64)
    private String donorUuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
    }

    public UserAccount(String fullName, String email, String passwordHash) {
        this(fullName, email, passwordHash, "ROLE_USER");
    }

    public UserAccount(String fullName, String email, String passwordHash, String role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = (role == null || role.isBlank()) ? "ROLE_USER" : role;
        this.donorUuid = java.util.UUID.randomUUID().toString();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
        }
        if (donorUuid == null || donorUuid.isBlank()) {
            donorUuid = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDonorUuid() {
        return donorUuid;
    }

    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isHospital() {
        return "ROLE_HOSPITAL".equalsIgnoreCase(role) || "HOSPITAL".equalsIgnoreCase(role);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
