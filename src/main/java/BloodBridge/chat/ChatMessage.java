package BloodBridge.chat;

import BloodBridge.auth.UserAccount;
import BloodBridge.request.BloodRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserAccount sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(BloodRequest bloodRequest, UserAccount sender, UserAccount recipient, String content) {
        this.bloodRequest = bloodRequest;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.isRead = false;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BloodRequest getBloodRequest() {
        return bloodRequest;
    }

    public UserAccount getSender() {
        return sender;
    }

    public UserAccount getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
