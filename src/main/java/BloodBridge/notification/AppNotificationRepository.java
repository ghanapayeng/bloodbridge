package BloodBridge.notification;

import BloodBridge.auth.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByRecipientOrderByCreatedAtDesc(UserAccount recipient);

    List<AppNotification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientAndIsReadFalse(UserAccount recipient);

    long countByRecipient_IdAndIsReadFalse(Long recipientId);

    List<AppNotification> findByRelatedRequestId(Long relatedRequestId);
}
