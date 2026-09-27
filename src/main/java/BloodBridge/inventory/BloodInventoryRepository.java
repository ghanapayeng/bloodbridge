package BloodBridge.inventory;

import BloodBridge.common.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {

    List<BloodInventory> findByStatus(InventoryStatus status);

    List<BloodInventory> findByFacilityNameIgnoreCase(String facilityName);

    List<BloodInventory> findByFacilityNameIgnoreCaseAndStatus(String facilityName, InventoryStatus status);

    Optional<BloodInventory> findByBatchUnitId(String batchUnitId);

    List<BloodInventory> findByBloodGroupAndStatus(BloodGroup bloodGroup, InventoryStatus status);

    @Query("SELECT b FROM BloodInventory b WHERE b.status = :status AND b.expiryDate <= :targetDate")
    List<BloodInventory> findApproachingOrExpired(
            @Param("status") InventoryStatus status,
            @Param("targetDate") LocalDate targetDate);

    @Query("SELECT b.bloodGroup, SUM(b.quantity) FROM BloodInventory b WHERE b.status = 'AVAILABLE' GROUP BY b.bloodGroup")
    List<Object[]> sumAvailableQuantityByBloodGroup();
}
