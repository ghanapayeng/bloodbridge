package BloodBridge.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class NotificationDtos {

    public record NotificationResponse(
            Long id,
            NotificationType type,
            String typeDisplay,
            String title,
            String message,
            Long relatedRequestId,
            boolean isRead,
            Instant createdAt,
            String timeAgo
    ) {
        public static NotificationResponse fromEntity(AppNotification n) {
            String timeAgo = formatTimeAgo(n.getCreatedAt());
            return new NotificationResponse(
                    n.getId(),
                    n.getType(),
                    n.getType() != null ? n.getType().getDisplay() : "Notification",
                    n.getTitle(),
                    n.getMessage(),
                    n.getRelatedRequestId(),
                    n.isRead(),
                    n.getCreatedAt(),
                    timeAgo
            );
        }

        private static String formatTimeAgo(Instant time) {
            if (time == null) return "";
            Duration diff = Duration.between(time, Instant.now());
            long minutes = diff.toMinutes();
            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + "m ago";
            long hours = diff.toHours();
            if (hours < 24) return hours + "h ago";
            long days = diff.toDays();
            return days + "d ago";
        }
    }

    public record NotificationSummaryResponse(
            long unreadCount,
            List<NotificationResponse> notifications
    ) {}
}
