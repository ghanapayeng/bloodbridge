package BloodBridge.common;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String display;

    BloodGroup(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    @JsonCreator
    public static BloodGroup from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase()
                .replace(" ", "_")
                .replace("-", "_NEGATIVE")
                .replace("+", "_POSITIVE");

        // Handle direct matches after normalization (e.g., A+ -> A_POSITIVE, A- -> A_NEGATIVE)
        for (BloodGroup bg : values()) {
            if (bg.name().equalsIgnoreCase(normalized) || bg.display.equalsIgnoreCase(value.trim())) {
                return bg;
            }
        }

        // Also check if original matches name
        for (BloodGroup bg : values()) {
            if (bg.name().equalsIgnoreCase(value.trim())) {
                return bg;
            }
        }

        throw new IllegalArgumentException("Unknown blood group: " + value);
    }
}

