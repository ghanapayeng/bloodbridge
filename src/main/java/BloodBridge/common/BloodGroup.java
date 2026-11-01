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
        String clean = value.trim();
        // Handle URL query parameter decoding where '+' was decoded to space (e.g. "O ", "A ", "B ", "AB ")
        if (value.endsWith(" ") && !clean.endsWith("+") && !clean.endsWith("-") && !clean.toUpperCase().endsWith("NEGATIVE") && !clean.toUpperCase().endsWith("POSITIVE")) {
            clean = clean + "+";
        }
        String normalized = clean.toUpperCase()
                .replace(" ", "_")
                .replace("-", "_NEGATIVE")
                .replace("+", "_POSITIVE");

        // Handle direct matches after normalization (e.g., A+ -> A_POSITIVE, A- -> A_NEGATIVE)
        for (BloodGroup bg : values()) {
            if (bg.name().equalsIgnoreCase(normalized) || bg.display.equalsIgnoreCase(clean)) {
                return bg;
            }
        }

        // Also check if original matches name
        for (BloodGroup bg : values()) {
            if (bg.name().equalsIgnoreCase(clean) || bg.name().equalsIgnoreCase(normalized)) {
                return bg;
            }
        }

        throw new IllegalArgumentException("Unknown blood group: " + value);
    }
}

