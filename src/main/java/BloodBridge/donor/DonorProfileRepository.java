package BloodBridge.donor;

import BloodBridge.auth.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {

    Optional<DonorProfile> findByUser(UserAccount user);

    Optional<DonorProfile> findByUser_Id(Long userId);

    List<DonorProfile> findByAvailableTrueAndCityContainingIgnoreCase(String city);
}
