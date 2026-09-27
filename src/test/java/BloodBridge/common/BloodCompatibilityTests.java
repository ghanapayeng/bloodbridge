package BloodBridge.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloodCompatibilityTests {

    @Test
    void allowsCompatibleRedCellDonations() {
        assertTrue(BloodCompatibility.canDonateTo(BloodGroup.O_NEGATIVE, BloodGroup.AB_POSITIVE));
        assertTrue(BloodCompatibility.canDonateTo(BloodGroup.A_POSITIVE, BloodGroup.A_POSITIVE));
        assertTrue(BloodCompatibility.canDonateTo(BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE));
    }

    @Test
    void rejectsIncompatibleRedCellDonations() {
        assertFalse(BloodCompatibility.canDonateTo(BloodGroup.A_POSITIVE, BloodGroup.O_POSITIVE));
        assertFalse(BloodCompatibility.canDonateTo(BloodGroup.B_POSITIVE, BloodGroup.A_POSITIVE));
        assertFalse(BloodCompatibility.canDonateTo(BloodGroup.AB_POSITIVE, BloodGroup.O_NEGATIVE));
    }
}
