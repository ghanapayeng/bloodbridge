package BloodBridge.donation;

import BloodBridge.auth.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByDonorOrderByDonatedAtDesc(UserAccount donor);
}
