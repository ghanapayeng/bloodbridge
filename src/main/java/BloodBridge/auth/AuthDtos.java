package BloodBridge.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 12, max = 72) String password,
            String phone,
            String emailOtp,
            String phoneOtp) {

        public RegisterRequest(String fullName, String email, String password) {
            this(fullName, email, password, null, null, null);
        }
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {
    }

    public record OtpLoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 6, max = 6) String code) {
    }

    public record UserResponse(Long id, String fullName, String email, Instant createdAt, String role) {

        public UserResponse(Long id, String fullName, String email, Instant createdAt) {
            this(id, fullName, email, createdAt, "ROLE_USER");
        }

        public static UserResponse from(UserAccount user) {
            return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt(), user.getRole());
        }
    }

    public record SendOtpRequest(
            @NotBlank @Size(max = 254) String identifier,
            @NotBlank @Size(max = 32) String type) {
    }

    public record VerifyOtpRequest(
            @NotBlank @Size(max = 254) String identifier,
            @NotBlank @Size(max = 32) String type,
            @NotBlank @Size(min = 6, max = 6) String code) {
    }

    public record OtpResponse(boolean success, String message, String debugCode) {
    }
}
