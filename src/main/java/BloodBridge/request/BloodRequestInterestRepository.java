package BloodBridge.request;

import BloodBridge.auth.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodRequestInterestRepository extends JpaRepository<BloodRequestInterest, Long> {

    boolean existsByBloodRequest_IdAndDonor_Id(Long bloodRequestId, Long donorId);

    List<BloodRequestInterest> findByBloodRequestOrderByCreatedAtDesc(BloodRequest bloodRequest);

    List<BloodRequestInterest> findByDonorOrderByCreatedAtDesc(UserAccount donor);
}
