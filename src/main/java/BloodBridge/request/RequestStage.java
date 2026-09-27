package BloodBridge.request;

public enum RequestStage {
    REQUEST_CREATED("Request Created"),
    MATCHING_DONORS("Matching Donors"),
    DONORS_CONTACTED("Donors Contacted"),
    DONOR_CONFIRMED("Donor Confirmed"),
    BLOOD_COLLECTED("Blood Collected"),
    HOSPITAL_RECEIVED("Hospital Received"),
    FULFILLED("Fulfilled");

    private final String display;

    RequestStage(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }
}
