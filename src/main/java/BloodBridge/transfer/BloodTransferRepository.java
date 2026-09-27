package BloodBridge.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BloodTransferRepository extends JpaRepository<BloodTransfer, Long> {

    List<BloodTransfer> findByStatusOrderByCreatedAtDesc(TransferStatus status);

    List<BloodTransfer> findBySourceHospitalIgnoreCaseOrDestinationHospitalIgnoreCaseOrderByCreatedAtDesc(
            String sourceHospital, String destinationHospital);

    List<BloodTransfer> findAllByOrderByCreatedAtDesc();
}
