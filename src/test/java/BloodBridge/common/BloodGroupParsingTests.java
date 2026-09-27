package BloodBridge.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BloodGroupParsingTests {

    @Test
    void parsesDisplayNotations() {
        assertEquals(BloodGroup.A_POSITIVE, BloodGroup.from("A+"));
        assertEquals(BloodGroup.A_NEGATIVE, BloodGroup.from("A-"));
        assertEquals(BloodGroup.B_POSITIVE, BloodGroup.from("B+"));
        assertEquals(BloodGroup.B_NEGATIVE, BloodGroup.from("B-"));
        assertEquals(BloodGroup.AB_POSITIVE, BloodGroup.from("AB+"));
        assertEquals(BloodGroup.AB_NEGATIVE, BloodGroup.from("AB-"));
        assertEquals(BloodGroup.O_POSITIVE, BloodGroup.from("O+"));
        assertEquals(BloodGroup.O_NEGATIVE, BloodGroup.from("O-"));
    }

    @Test
    void parsesStandardEnumNames() {
        assertEquals(BloodGroup.A_POSITIVE, BloodGroup.from("A_POSITIVE"));
        assertEquals(BloodGroup.B_NEGATIVE, BloodGroup.from("B_NEGATIVE"));
        assertEquals(BloodGroup.O_POSITIVE, BloodGroup.from("o_positive"));
    }

    @Test
    void handlesNullOrBlank() {
        assertNull(BloodGroup.from(null));
        assertNull(BloodGroup.from("   "));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> BloodGroup.from("INVALID"));
    }
}
