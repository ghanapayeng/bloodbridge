package BloodBridge.call;

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
@Table(name = "call_signals")
public class CallSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blood_request_id", nullable = false)
    private BloodRequest bloodRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caller_id", nullable = false)
    private UserAccount caller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @Column(name = "signal_type", nullable = false, length = 32)
    private String signalType;

    @Column(name = "signal_data", columnDefinition = "LONGTEXT")
    private String signalData;

    @Column(name = "is_consumed", nullable = false)
    private boolean isConsumed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CallSignal() {
    }

    public CallSignal(
            BloodRequest bloodRequest,
            UserAccount caller,
            UserAccount recipient,
            String signalType,
            String signalData) {
        this.bloodRequest = bloodRequest;
        this.caller = caller;
        this.recipient = recipient;
        this.signalType = signalType;
        this.signalData = signalData;
        this.isConsumed = false;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public BloodRequest getBloodRequest() {
        return bloodRequest;
    }

    public UserAccount getCaller() {
        return caller;
    }

    public UserAccount getRecipient() {
        return recipient;
    }

    public String getSignalType() {
        return signalType;
    }

    public String getSignalData() {
        return signalData;
    }

    public boolean isConsumed() {
        return isConsumed;
    }

    public void setConsumed(boolean consumed) {
        isConsumed = consumed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
