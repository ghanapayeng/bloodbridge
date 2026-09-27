package BloodBridge.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record SendMessageRequest(
            @NotBlank @Size(max = 4000) String content) {
    }

    public record ChatMessageResponse(
            Long id,
            Long requestId,
            Long senderId,
            String senderName,
            Long recipientId,
            String recipientName,
            String content,
            boolean isRead,
            boolean isMine,
            Instant createdAt) {

        public static ChatMessageResponse from(ChatMessage message, Long currentUserId) {
            return new ChatMessageResponse(
                    message.getId(),
                    message.getBloodRequest().getId(),
                    message.getSender().getId(),
                    message.getSender().getFullName(),
                    message.getRecipient().getId(),
                    message.getRecipient().getFullName(),
                    message.getContent(),
                    message.isRead(),
                    message.getSender().getId().equals(currentUserId),
                    message.getCreatedAt());
        }
    }

    public record UnreadCountResponse(long count) {
    }
}
