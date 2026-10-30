package BloodBridge.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BloodComponent {
    WHOLE_BLOOD("Whole Blood", "WB", 35),
    PRBC("PRBC", "PRBC", 42),
    FFP("FFP", "FFP", 365),
    PLATELETS("Platelets", "PLT", 5),
    CRYOPRECIPITATE("Cryoprecipitate", "CRYO", 365);

    private final String displayName;
    private final String code;
    private final int shelfLifeDays;

    BloodComponent(String displayName, String code, int shelfLifeDays) {
        this.displayName = displayName;
        this.code = code;
        this.shelfLifeDays = shelfLifeDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public int getShelfLifeDays() {
        return shelfLifeDays;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static BloodComponent from(String value) {
        if (value == null || value.isBlank()) {
            return WHOLE_BLOOD;
        }
        String clean = value.trim().toUpperCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace("(", "")
                .replace(")", "");

        for (BloodComponent c : values()) {
            if (c.name().equalsIgnoreCase(clean)
                    || c.code.equalsIgnoreCase(clean)
                    || c.displayName.equalsIgnoreCase(value.trim())) {
                return c;
            }
        }
        if (clean.contains("PRBC") || clean.contains("PACKED") || clean.contains("RED_BLOOD")) {
            return PRBC;
        }
        if (clean.contains("FFP") || clean.contains("PLASMA") || clean.contains("FROZEN")) {
            return FFP;
        }
        if (clean.contains("PLATELET") || clean.contains("PLT")) {
            return PLATELETS;
        }
        if (clean.contains("CRYO")) {
            return CRYOPRECIPITATE;
        }
        return WHOLE_BLOOD;
    }
}
