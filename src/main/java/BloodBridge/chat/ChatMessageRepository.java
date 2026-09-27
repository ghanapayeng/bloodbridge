package BloodBridge.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m " +
           "JOIN FETCH m.sender s " +
           "JOIN FETCH m.recipient r " +
           "WHERE m.bloodRequest.id = :requestId " +
           "AND ((s.id = :userA AND r.id = :userB) OR (s.id = :userB AND r.id = :userA)) " +
           "ORDER BY m.createdAt ASC")
    List<ChatMessage> findConversation(
            @Param("requestId") Long requestId,
            @Param("userA") Long userA,
            @Param("userB") Long userB);

    long countByRecipient_IdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
           "WHERE m.bloodRequest.id = :requestId " +
           "AND m.recipient.id = :recipientId " +
           "AND m.sender.id = :senderId " +
           "AND m.isRead = false")
    void markAsRead(
            @Param("requestId") Long requestId,
            @Param("recipientId") Long recipientId,
            @Param("senderId") Long senderId);
}
