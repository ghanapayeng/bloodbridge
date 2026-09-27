package BloodBridge.notification;

import BloodBridge.notification.NotificationDtos.NotificationResponse;
import BloodBridge.notification.NotificationDtos.NotificationSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationSummaryResponse> getMyNotifications(Authentication authentication) {
        Long userId = notificationService.getUserIdByEmail(authentication.getName());
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = notificationService.getUserIdByEmail(authentication.getName());
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            Authentication authentication,
            @PathVariable("id") Long id) {
        Long userId = notificationService.getUserIdByEmail(authentication.getName());
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(Authentication authentication) {
        Long userId = notificationService.getUserIdByEmail(authentication.getName());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }
}
