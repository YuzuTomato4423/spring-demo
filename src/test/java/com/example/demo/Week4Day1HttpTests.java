package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.context.ApplicationContext;
import com.example.demo.controller.Week4Day3EmployeeController;
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 実HTTPでURL割当、引数変換、JSON本文、必須queryのエラーを検証する。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("w4d1")
class Week4Day1HttpTests {
    @Autowired private Environment environment;
    @Autowired private ApplicationContext context;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test void helloKeepsConfiguredResponse() throws Exception {
        var response = get("/hello");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(environment.getRequiredProperty("app.message"));
    }

    @Test void differentPathIdsReachController() throws Exception {
        for (int id : new int[] {1, 2}) {
            var response = get("/employees/" + id);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/json");
            JsonNode body = json.readTree(response.body());
            assertThat(body.get("id").asInt()).isEqualTo(id);
            assertThat(body.get("message").asText()).contains("仮応答");
        }
    }

    @Test void departmentIsPassedAndNameCanBeOmitted() throws Exception {
        for (String department : new String[] {"開発部", "営業部"}) {
            var response = get("/employees?department=" + encode(department));
            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode body = json.readTree(response.body());
            assertThat(body.get("department").asText()).isEqualTo(department);
            assertThat(body.get("name").isNull()).isTrue();
            assertThat(body.get("message").asText()).contains("仮応答");
        }
    }

    @Test void optionalJapaneseNameIsPassed() throws Exception {
        var response = get("/employees?department=" + encode("開発部") + "&name=" + encode("田中"));
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("department").asText()).isEqualTo("開発部");
        assertThat(body.get("name").asText()).isEqualTo("田中");
    }

    @Test void missingRequiredDepartmentReturns400() throws Exception {
        assertThat(get("/employees").statusCode()).isEqualTo(400);
        assertThat(get("/employees?name=Tanaka").statusCode()).isEqualTo(400);
    }

    @Test void nonNumericPathIdReturns400() throws Exception {
        assertThat(get("/employees/abc").statusCode()).isEqualTo(400);
    }

    @Test void unsupportedMethodReturns405() throws Exception {
        assertThat(context.getBeansOfType(Week4Day3EmployeeController.class)).isEmpty();
        var request = HttpRequest.newBuilder(uri("/employees/1"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(405);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + environment.getRequiredProperty("local.server.port") + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
