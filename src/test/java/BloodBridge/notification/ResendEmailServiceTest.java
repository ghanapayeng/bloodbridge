package BloodBridge.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

class ResendEmailServiceTest {

    private static final String TEST_API_KEY = "re_live_secret_key_123456789";
    private static final String TEST_FROM_EMAIL = "onboarding@resend.dev";

    @Test
    void testSuccessfulEmailDispatchAndPayloadConstruction() {
        AtomicReference<MockClientHttpRequest> capturedRequest = new AtomicReference<>();

        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.resend.com/emails")
                .requestFactory((uri, httpMethod) -> {
                    MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
                    request.setResponse(new MockClientHttpResponse(
                            "{\"id\": \"msg_49a39f3b-0ee0-4495-a0f6-95ff37206140\"}".getBytes(StandardCharsets.UTF_8),
                            HttpStatusCode.valueOf(200)));
                    capturedRequest.set(request);
                    return request;
                })
                .build();

        ResendEmailService service = new ResendEmailService(restClient, TEST_API_KEY, TEST_FROM_EMAIL);

        // Execute email sending
        service.sendOtpEmail("donor.test@example.com", "849201", 5);

        // Assert request construction
        MockClientHttpRequest request = capturedRequest.get();
        assertNotNull(request, "HTTP request was not captured");
        assertEquals(HttpMethod.POST, request.getMethod());
        assertEquals("https://api.resend.com/emails", request.getURI().toString());
        assertEquals("Bearer " + TEST_API_KEY, request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("application/json", request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));

        // Assert payload content
        String body = request.getBodyAsString();
        assertTrue(body.contains("\"to\":[\"donor.test@example.com\"]"), "Payload must contain recipient");
        assertTrue(body.contains("BloodBridge <onboarding@resend.dev>"), "Payload must contain sender");
        assertTrue(body.contains("Your BloodBridge Verification Code"), "Payload must contain subject");
        assertTrue(body.contains("849201"), "Payload must contain the OTP code");
        assertTrue(body.contains("5 minutes"), "Payload must state 5-minute expiry");
    }

    @Test
    void testMissingApiKeyThrowsInternalServerError() {
        RestClient restClient = RestClient.builder().baseUrl("https://api.resend.com/emails").build();
        ResendEmailService service = new ResendEmailService(restClient, "", TEST_FROM_EMAIL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.sendOtpEmail("test@example.com", "123456");
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("RESEND_API_KEY"));
    }

    @Test
    void testBlankRecipientThrowsBadRequest() {
        RestClient restClient = RestClient.builder().baseUrl("https://api.resend.com/emails").build();
        ResendEmailService service = new ResendEmailService(restClient, TEST_API_KEY, TEST_FROM_EMAIL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.sendOtpEmail("   ", "123456");
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Recipient email address is required"));
    }

    @Test
    void testResendApiUnauthorized401MasksKeyAndThrowsInternalServerError() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.resend.com/emails")
                .requestFactory((uri, httpMethod) -> {
                    MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
                    request.setResponse(new MockClientHttpResponse(
                            "{\"statusCode\": 401, \"message\": \"API key is invalid\"}".getBytes(StandardCharsets.UTF_8),
                            HttpStatusCode.valueOf(401)));
                    return request;
                })
                .build();

        ResendEmailService service = new ResendEmailService(restClient, TEST_API_KEY, TEST_FROM_EMAIL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.sendOtpEmail("user@example.com", "654321");
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        // Verify sensitive credentials or raw provider details are not leaked
        assertFalse(ex.getReason().contains(TEST_API_KEY), "API key must never leak in error response");
        assertTrue(ex.getReason().contains("invalid or unauthorized"));
    }

    @Test
    void testResendApiForbidden403ProvidesClearDomainRestrictionMessage() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.resend.com/emails")
                .requestFactory((uri, httpMethod) -> {
                    MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
                    request.setResponse(new MockClientHttpResponse(
                            "{\"statusCode\": 403, \"message\": \"You can only send testing emails to your own email address.\"}".getBytes(StandardCharsets.UTF_8),
                            HttpStatusCode.valueOf(403)));
                    return request;
                })
                .build();

        ResendEmailService service = new ResendEmailService(restClient, TEST_API_KEY, TEST_FROM_EMAIL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.sendOtpEmail("unverified@example.com", "654321");
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You can only send testing emails"));
    }

    @Test
    void testResendApiRateLimit429ThrowsTooManyRequests() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.resend.com/emails")
                .requestFactory((uri, httpMethod) -> {
                    MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
                    request.setResponse(new MockClientHttpResponse(
                            "{\"statusCode\": 429, \"message\": \"Rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8),
                            HttpStatusCode.valueOf(429)));
                    return request;
                })
                .build();

        ResendEmailService service = new ResendEmailService(restClient, TEST_API_KEY, TEST_FROM_EMAIL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.sendOtpEmail("user@example.com", "654321");
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertTrue(ex.getReason().contains("rate limit"));
    }

    @Test
    void testResendApiServerError500ThrowsBadGateway() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.resend.com/emails")
                .requestFactory((uri, httpMethod) -> {
                    MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
                    request.setResponse(new MockClientHttpResponse(
                            "{\"statusCode\": 500, \"message\": \"Internal Server Error\"}".getBytes(StandardCharsets.UTF_8),
                            HttpStatusCode.valueOf(500)));
                    return request;
                })
                .build();

        ResendEmailService service = new ResendEmailService(restClient, TEST_API_KEY, TEST_FROM_EMAIL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.sendOtpEmail("user@example.com", "654321");
        });

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Failed to deliver verification email"));
    }

    @Test
    void testNetworkTimeoutThrowsGatewayTimeout() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.resend.com/emails")
                .requestFactory((uri, httpMethod) -> {
                    MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri) {
                        @Override
                        protected org.springframework.http.client.ClientHttpResponse executeInternal() throws IOException {
                            throw new ResourceAccessException("Connection timed out", new SocketTimeoutException("Read timed out"));
                        }
                    };
                    return request;
                })
                .build();

        ResendEmailService service = new ResendEmailService(restClient, TEST_API_KEY, TEST_FROM_EMAIL);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.sendOtpEmail("user@example.com", "654321");
        });

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("timed out"));
    }
}
