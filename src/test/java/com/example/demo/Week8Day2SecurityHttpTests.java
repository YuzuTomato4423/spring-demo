package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("spr-w8-d2")
class Week8Day2SecurityHttpTests {

    private static final String PASSWORD = UUID.randomUUID().toString();

    private final HttpClient client = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void securityUser(DynamicPropertyRegistry properties) {
        properties.add("spring.security.user.name", () -> "user");
        properties.add("spring.security.user.password", () -> PASSWORD);
    }

    @Test
    void helloIsPublic() throws Exception {
        var response = get("/hello", false);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Hello Spring!");
    }

    @Test
    void employeesRejectsAnonymousRequests() throws Exception {
        var response = get("/employees", false);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).hasValueSatisfying(
                challenge -> assertThat(challenge).startsWith("Basic "));
    }

    @Test
    void employeesReturnsListToAuthenticatedUser() throws Exception {
        var response = get("/employees", true);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    private HttpResponse<String> get(String path, boolean authenticated) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5));
        if (authenticated) {
            String credentials = Base64.getEncoder().encodeToString(
                    ("user:" + PASSWORD).getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + credentials);
        }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
