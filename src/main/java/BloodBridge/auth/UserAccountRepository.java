package BloodBridge.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<UserAccount> findByRole(String role);

    Optional<UserAccount> findByDonorUuid(String donorUuid);
}
