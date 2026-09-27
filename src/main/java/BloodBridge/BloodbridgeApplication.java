package BloodBridge;

import BloodBridge.config.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BloodbridgeApplication {

	public static void main(String[] args) {
		DotenvLoader.load();
		SpringApplication.run(BloodbridgeApplication.class, args);
	}

}
