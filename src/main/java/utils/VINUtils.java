package utils;

import java.security.SecureRandom;

public final class VINUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private VINUtils() {}

    public static String generateVin(int colorId, int versionId) {
        long timestamp = System.currentTimeMillis() % 100000000L;
        int rand = RANDOM.nextInt(100000); // 5 digits
        return String.format("VIN%02dV%03d-%08d-%05d", colorId % 100, versionId % 1000, timestamp, rand);
    }

    public static String generateVin() {
        long timestamp = System.currentTimeMillis() % 100000000L;
        int randA = RANDOM.nextInt(1000);
        int randB = RANDOM.nextInt(100000);
        return String.format("VINX-%03d-%08d-%05d", randA, timestamp, randB);
    }
}


