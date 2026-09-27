package BloodBridge.common;

public final class LocationUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private LocationUtils() {
    }

    /**
     * Calculates the great-circle distance between two points using the Haversine formula.
     * Returns distance in kilometers, or null if any coordinate is missing.
     */
    public static Double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double originLat = Math.toRadians(lat1);
        double destLat = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(originLat) * Math.cos(destLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;
        return Math.round(distance * 10.0) / 10.0;
    }

    /**
     * Formats approximate distance for user privacy (e.g., "2.4 km away").
     * Never reveals exact street addresses.
     */
    public static String formatApproximateDistance(Double distanceKm, String city1, String city2) {
        if (distanceKm != null) {
            return distanceKm + " km away";
        }
        if (city1 != null && city2 != null && !city1.isBlank() && !city2.isBlank()) {
            if (city1.trim().equalsIgnoreCase(city2.trim())) {
                return "Nearby in " + city1.trim();
            }
            return "In " + city2.trim();
        }
        return "Distance approximate";
    }
}
