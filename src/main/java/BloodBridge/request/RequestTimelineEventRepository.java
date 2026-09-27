package BloodBridge.request;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RequestTimelineEventRepository extends JpaRepository<RequestTimelineEvent, Long> {

    List<RequestTimelineEvent> findByBloodRequestOrderByCreatedAtAsc(BloodRequest bloodRequest);

    List<RequestTimelineEvent> findByBloodRequest_IdOrderByCreatedAtAsc(Long requestId);
}
