package BloodBridge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.session.SessionRepository;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BloodbridgeApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
		assertNotNull(context.getBean(SessionRepository.class), "SessionRepository must be configured for JDBC session persistence");
	}

}
