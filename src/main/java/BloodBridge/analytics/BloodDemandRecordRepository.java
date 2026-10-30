package BloodBridge.analytics;

import BloodBridge.common.BloodComponent;
import BloodBridge.common.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BloodDemandRecordRepository extends JpaRepository<BloodDemandRecord, Long> {

    List<BloodDemandRecord> findByIsForecastOrderByPeriodDateAsc(boolean isForecast);

    List<BloodDemandRecord> findAllByOrderByPeriodDateAsc();

    List<BloodDemandRecord> findByBloodGroupOrderByPeriodDateAsc(BloodGroup bloodGroup);

    List<BloodDemandRecord> findByComponentOrderByPeriodDateAsc(BloodComponent component);

    long countByIsDemoTrue();

    @Query("SELECT r.periodYear, r.periodMonth, r.isForecast, SUM(r.unitsDemanded), SUM(r.unitsSupplied) " +
           "FROM BloodDemandRecord r " +
           "GROUP BY r.periodYear, r.periodMonth, r.isForecast " +
           "ORDER BY r.periodYear ASC, r.periodMonth ASC")
    List<Object[]> aggregateMonthlyDemand();

    @Query("SELECT r.bloodGroup, SUM(r.unitsDemanded) " +
           "FROM BloodDemandRecord r " +
           "WHERE r.periodDate >= :startDate " +
           "GROUP BY r.bloodGroup")
    List<Object[]> sumDemandByBloodGroupSince(@Param("startDate") LocalDate startDate);

    @Query("SELECT r.component, SUM(r.unitsDemanded) " +
           "FROM BloodDemandRecord r " +
           "WHERE r.periodDate >= :startDate " +
           "GROUP BY r.component")
    List<Object[]> sumDemandByComponentSince(@Param("startDate") LocalDate startDate);
}
