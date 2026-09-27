package BloodBridge.request;

public enum RequestUrgency {
    LOW,
    NORMAL,
    MEDIUM,
    HIGH,
    URGENT,
    CRITICAL;

    public boolean isEmergency() {
        return this == URGENT || this == CRITICAL || this == HIGH;
    }
}
