package BloodBridge.common;

import java.util.EnumSet;

public final class BloodCompatibility {

    private BloodCompatibility() {
    }

    /**
     * Returns whether a donor's red blood cells are compatible with the recipient's blood group.
     * Final medical eligibility and compatibility checks must still be performed by clinicians.
     */
    public static boolean canDonateTo(BloodGroup donor, BloodGroup recipient) {
        return switch (recipient) {
            case O_NEGATIVE -> donor == BloodGroup.O_NEGATIVE;
            case O_POSITIVE -> EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE).contains(donor);
            case A_NEGATIVE -> EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE).contains(donor);
            case A_POSITIVE -> EnumSet.of(
                    BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.A_NEGATIVE, BloodGroup.A_POSITIVE).contains(donor);
            case B_NEGATIVE -> EnumSet.of(BloodGroup.O_NEGATIVE, BloodGroup.B_NEGATIVE).contains(donor);
            case B_POSITIVE -> EnumSet.of(
                    BloodGroup.O_NEGATIVE, BloodGroup.O_POSITIVE, BloodGroup.B_NEGATIVE, BloodGroup.B_POSITIVE).contains(donor);
            case AB_NEGATIVE -> EnumSet.of(
                    BloodGroup.O_NEGATIVE, BloodGroup.A_NEGATIVE, BloodGroup.B_NEGATIVE, BloodGroup.AB_NEGATIVE).contains(donor);
            case AB_POSITIVE -> true;
        };
    }
}
