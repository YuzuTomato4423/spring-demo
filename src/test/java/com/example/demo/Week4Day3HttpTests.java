package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.notification.EmployeeRegistrationNotifier;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.application.name=day3-http-tests")
@ActiveProfiles("carry-demo")
class Week4Day3HttpTests {
    @Autowired private Environment environment;
    @Autowired private EmployeeRegistrationNotifier notifier;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test void lifecyclePreservesExistingEmployeesAndNotifiesOnlyOnRegistration() throws Exception {
        int beforeCount = count();
        int beforeNotifications = notifier.getDeliveryCount();
        var created = send("POST", "/employees", body("D3社員", 1));
        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode dto = json.readTree(created.body());
        int id = dto.get("id").asInt();
        assertThat(id).isGreaterThan(6);
        String path = "/employees/" + id;
        assertThat(created.headers().firstValue("location")).contains(path);
        assertDto(dto, id, "D3社員", "開発部");
        assertThat(count()).isEqualTo(beforeCount + 1);
        assertDto(json.readTree(send("GET", path, null).body()), id, "D3社員", "開発部");
        var updated = send("PUT", path, body("D3更新社員", 2));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertDto(json.readTree(updated.body()), id, "D3更新社員", "営業部");
        assertDto(json.readTree(send("GET", path, null).body()), id, "D3更新社員", "営業部");
        assertThat(count()).isEqualTo(beforeCount + 1);
        assertThat(notifier.getDeliveryCount()).isEqualTo(beforeNotifications + 1);
        var deleted = send("DELETE", path, null);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(deleted.body()).isEmpty();
        assertThat(send("GET", path, null).statusCode()).isEqualTo(404);
        assertThat(send("DELETE", path, null).statusCode()).isEqualTo(404);
        assertThat(count()).isEqualTo(beforeCount);
        var next = send("POST", "/employees", body("次の社員", 1));
        assertThat(next.statusCode()).isEqualTo(201);
        int nextId = json.readTree(next.body()).get("id").asInt();
        assertThat(nextId).isGreaterThan(id);
        assertThat(send("DELETE", "/employees/" + nextId, null).statusCode()).isEqualTo(204);
        assertDto(json.readTree(send("GET", "/employees/1", null).body()), 1, "田中", "開発部");
    }

    @Test void invalidCreateAndUpdateNeverChangeDataOrNotify() throws Exception {
        int beforeCount = count();
        int beforeNotifications = notifier.getDeliveryCount();
        for (String invalid : new String[]{body("", 1), body("　 ", 1), body("社員", 999),
                "{\"name\":null,\"departmentId\":1}", "{}", "{", "null"}) {
            assertThat(send("POST", "/employees", invalid).statusCode()).isEqualTo(400);
            assertThat(send("PUT", "/employees/1", invalid).statusCode()).isEqualTo(400);
        }
        assertThat(count()).isEqualTo(beforeCount);
        assertThat(notifier.getDeliveryCount()).isEqualTo(beforeNotifications);
        assertDto(json.readTree(send("GET", "/employees/1", null).body()), 1, "田中", "開発部");
    }

    @Test void missingAndNonNumericIdsDoNotCreateOrDeleteEmployees() throws Exception {
        int beforeCount = count();
        assertThat(send("PUT", "/employees/999999", body("社員", 1)).statusCode()).isEqualTo(404);
        assertThat(send("DELETE", "/employees/999999", null).statusCode()).isEqualTo(404);
        assertThat(send("PUT", "/employees/abc", body("社員", 1)).statusCode()).isEqualTo(400);
        assertThat(send("DELETE", "/employees/abc", null).statusCode()).isEqualTo(400);
        assertThat(count()).isEqualTo(beforeCount);
    }

    private int count() throws Exception {
        var response = send("GET", "/employees", null);
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body()).size();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getRequiredProperty("local.server.port") + path)).timeout(Duration.ofSeconds(5));
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String body(String name, int department) {
        return "{\"name\":\"" + name + "\",\"departmentId\":" + department + "}";
    }

    private static void assertDto(JsonNode dto, int id, String name, String department) {
        assertThat(dto.size()).isEqualTo(3);
        assertThat(dto.get("id").asInt()).isEqualTo(id);
        assertThat(dto.get("name").asText()).isEqualTo(name);
        assertThat(dto.get("departmentName").asText()).isEqualTo(department);
    }
}
