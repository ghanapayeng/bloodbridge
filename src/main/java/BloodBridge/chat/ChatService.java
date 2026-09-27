package BloodBridge.chat;

import BloodBridge.auth.AuthService;
import BloodBridge.auth.UserAccount;
import BloodBridge.auth.UserAccountRepository;
import BloodBridge.chat.ChatDtos.ChatMessageResponse;
import BloodBridge.request.BloodRequest;
import BloodBridge.request.BloodRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthService authService;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            BloodRequestRepository bloodRequestRepository,
            UserAccountRepository userAccountRepository,
            AuthService authService) {
        this.chatMessageRepository = chatMessageRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.authService = authService;
    }

    @Transactional
    public List<ChatMessageResponse> getConversation(String currentUserEmail, long requestId, long otherUserId) {
        UserAccount currentUser = authService.requireByEmail(currentUserEmail);
        BloodRequest request = requireRequest(requestId);
        UserAccount otherUser = userAccountRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        // Mark incoming messages as read
        chatMessageRepository.markAsRead(requestId, currentUser.getId(), otherUser.getId());

        List<ChatMessage> messages = chatMessageRepository.findConversation(requestId, currentUser.getId(), otherUser.getId());
        return messages.stream()
                .map(m -> ChatMessageResponse.from(m, currentUser.getId()))
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(String currentUserEmail, long requestId, long recipientId, String content) {
        UserAccount sender = authService.requireByEmail(currentUserEmail);
        BloodRequest request = requireRequest(requestId);
        UserAccount recipient = userAccountRepository.findById(recipientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found."));

        if (sender.getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send message to yourself.");
        }

        ChatMessage message = new ChatMessage(request, sender, recipient, content.trim());
        ChatMessage saved = chatMessageRepository.save(message);
        return ChatMessageResponse.from(saved, sender.getId());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String currentUserEmail) {
        UserAccount currentUser = authService.requireByEmail(currentUserEmail);
        return chatMessageRepository.countByRecipient_IdAndIsReadFalse(currentUser.getId());
    }

    private BloodRequest requireRequest(long requestId) {
        return bloodRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found."));
    }
}
