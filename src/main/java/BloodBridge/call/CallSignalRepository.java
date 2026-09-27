package BloodBridge.call;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CallSignalRepository extends JpaRepository<CallSignal, Long> {

    @Query("SELECT s FROM CallSignal s " +
           "JOIN FETCH s.caller c " +
           "JOIN FETCH s.recipient r " +
           "JOIN FETCH s.bloodRequest b " +
           "WHERE s.recipient.id = :recipientId AND s.isConsumed = false " +
           "ORDER BY s.createdAt ASC")
    List<CallSignal> findPendingSignalsForRecipient(@Param("recipientId") Long recipientId);

    @Modifying
    @Query("UPDATE CallSignal s SET s.isConsumed = true WHERE s.recipient.id = :recipientId AND s.isConsumed = false")
    void markConsumedForRecipient(@Param("recipientId") Long recipientId);
}
