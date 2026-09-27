package BloodBridge.donor;

public enum EligibilityStatus {
    ELIGIBLE("Eligible to donate"),
    TEMPORARILY_UNAVAILABLE("Temporarily unavailable"),
    REQUIRES_VERIFICATION("Requires verification");

    private final String display;

    EligibilityStatus(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
