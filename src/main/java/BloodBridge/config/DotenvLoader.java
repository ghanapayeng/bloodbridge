package BloodBridge.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Lightweight environment variable loader for .env files.
 * Automatically loads .env variables into System properties before Spring Boot startup,
 * keeping database credentials and secrets secure and out of version control.
 */
public final class DotenvLoader {

    private DotenvLoader() {
    }

    public static void load() {
        File envFile = new File(".env");
        if (!envFile.exists() || !envFile.isFile()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Ignore empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                // Strip matching quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) ||
                    (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)) {
                    value = value.substring(1, value.length() - 1);
                }

                // Set as system property if not already defined (environment takes precedence)
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Notice: Could not load .env file: " + e.getMessage());
        }
    }
}
