package BloodBridge.chat;

import BloodBridge.chat.ChatDtos.ChatMessageResponse;
import BloodBridge.chat.ChatDtos.SendMessageRequest;
import BloodBridge.chat.ChatDtos.UnreadCountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/requests/{requestId}/conversations/{otherUserId}")
    public List<ChatMessageResponse> getConversation(
            Authentication authentication,
            @PathVariable long requestId,
            @PathVariable long otherUserId) {
        return chatService.getConversation(authentication.getName(), requestId, otherUserId);
    }

    @PostMapping("/requests/{requestId}/conversations/{otherUserId}")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            Authentication authentication,
            @PathVariable long requestId,
            @PathVariable long otherUserId,
            @Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse response = chatService.sendMessage(authentication.getName(), requestId, otherUserId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(Authentication authentication) {
        return new UnreadCountResponse(chatService.getUnreadCount(authentication.getName()));
    }
}
