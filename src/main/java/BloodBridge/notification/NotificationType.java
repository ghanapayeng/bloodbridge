package BloodBridge.notification;

public enum NotificationType {
    DONOR_MATCH("Compatible Donor Found"),
    EMERGENCY_REQUEST("Emergency Blood Request"),
    STATUS_UPDATE("Request Status Update"),
    DONOR_RESPONSE("Donor Response"),
    LOW_INVENTORY("Low Inventory Alert"),
    EXPIRY_WARNING("Blood Expiry Warning"),
    ELIGIBILITY_UPDATE("Eligibility Update"),
    TRANSFER_REQUEST("Hospital Transfer Request"),
    SECURITY_ALERT("Security Alert");

    private final String display;

    NotificationType(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
