package BloodBridge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/**
 * Startup diagnostic listener that verifies and reports the actual listening port,
 * bound network address, active profiles, and precise startup duration.
 */
@Component
public class StartupDiagnostics implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnostics.class);

    private final Environment environment;
    private int listeningPort = -1;

    public StartupDiagnostics(Environment environment) {
        this.environment = environment;
    }

    @EventListener
    public void onWebServerInitialized(WebServerInitializedEvent event) {
        this.listeningPort = event.getWebServer().getPort();
        String serverAddress = environment.getProperty("server.address", "0.0.0.0");
        log.info("================================================================================");
        log.info("[STARTUP DIAGNOSTIC] Web server initialized: Tomcat bound to {}:{}", serverAddress, listeningPort);
        log.info("================================================================================");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Duration timeTaken = event.getTimeTaken();
        long millis = timeTaken != null ? timeTaken.toMillis() : -1;
        String durationStr = millis >= 0 ? String.format("%.2f s (%d ms)", millis / 1000.0, millis) : "unknown";
        String serverAddress = environment.getProperty("server.address", "0.0.0.0");
        String activeProfiles = Arrays.toString(environment.getActiveProfiles());

        log.info("================================================================================");
        log.info("[STARTUP DIAGNOSTIC] BloodBridge Application is READY to accept traffic!");
        log.info("[STARTUP DIAGNOSTIC] Actual Listening Port: {}", listeningPort);
        log.info("[STARTUP DIAGNOSTIC] Bound Address:         {}", serverAddress);
        log.info("[STARTUP DIAGNOSTIC] Active Profiles:       {}", activeProfiles);
        log.info("[STARTUP DIAGNOSTIC] Total Startup Time:    {}", durationStr);
        log.info("[STARTUP DIAGNOSTIC] Health Endpoint:       http://{}:{}/api/health",
                "0.0.0.0".equals(serverAddress) ? "localhost" : serverAddress, listeningPort);
        log.info("================================================================================");
    }
}
