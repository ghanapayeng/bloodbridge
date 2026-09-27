package BloodBridge.notification;

import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.notification.NotificationDtos.NotificationResponse;
import BloodBridge.notification.NotificationDtos.NotificationSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    public NotificationService(
            AppNotificationRepository notificationRepository,
            UserAccountRepository userAccountRepository) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public AppNotification sendNotification(
            UserAccount recipient,
            NotificationType type,
            String title,
            String message,
            Long relatedRequestId) {
        AppNotification notification = new AppNotification(recipient, type, title, message, relatedRequestId);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getUserNotifications(Long userId) {
        List<AppNotification> list = notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId);
        long unread = notificationRepository.countByRecipient_IdAndIsReadFalse(userId);
        List<NotificationResponse> responses = list.stream().map(NotificationResponse::fromEntity).toList();
        return new NotificationSummaryResponse(unread, responses);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipient_IdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Cannot modify notifications belonging to another user.");
        }

        notification.setRead(true);
        AppNotification updated = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(updated);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<AppNotification> unread = notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead())
                .toList();

        for (AppNotification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        return userAccountRepository.findByEmail(email)
                .map(UserAccount::getId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
}
