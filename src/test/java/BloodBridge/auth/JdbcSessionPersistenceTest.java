package BloodBridge.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(scripts = "classpath:org/springframework/session/jdbc/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class JdbcSessionPersistenceTest {

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Test
    void testJdbcSessionCreationAndRetrieval() {
        assertNotNull(sessionRepository, "SessionRepository must be registered");

        Session session = sessionRepository.createSession();
        String sessionId = session.getId();

        SecurityContext securityContext = new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken("donor@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        session.setAttribute("userEmail", "donor@example.com");

        // Save session through repository
        @SuppressWarnings("unchecked")
        SessionRepository<Session> typedRepo = (SessionRepository<Session>) sessionRepository;
        typedRepo.save(session);

        // Find session by ID
        Session loaded = typedRepo.findById(sessionId);
        assertNotNull(loaded, "Session must be retrieved from JDBC repository");
        assertEquals("donor@example.com", loaded.getAttribute("userEmail"));
        assertNotNull(loaded.getAttribute("SPRING_SECURITY_CONTEXT"));
    }
}
