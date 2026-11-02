package BloodBridge;

import BloodBridge.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@SpringBootApplication
@EnableScheduling
@EnableJdbcHttpSession
public class BloodbridgeApplication {

	public static void main(String[] args) {
		DotenvLoader.load();
		SpringApplication.run(BloodbridgeApplication.class, args);
	}

}
