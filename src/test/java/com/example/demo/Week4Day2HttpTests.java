package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import com.example.demo.controller.Week4Day1EmployeeController;
import com.example.demo.controller.Week4Day2EmployeeController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 実HTTPで検索の意味、DTOの公開境界、エラーとD1切替を検証する。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("carry-demo")
class Week4Day2HttpTests {
    @Autowired private Environment environment;
    @Autowired private ApplicationContext context;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test void listUsesSixStoredEmployeesAndResponseDto() throws Exception {
        JsonNode body = list("/employees", 6);
        assertThat(body.get(0).get("id").asInt()).isEqualTo(1);
        assertThat(body.get(5).get("name").asText()).isEqualTo("山田");
        for (JsonNode employee : body) {
            assertDto(employee);
        }
    }

    @Test void detailUsesExistingServiceAndResponseDto() throws Exception {
        var response = get("/employees/1");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("id").asInt()).isEqualTo(1);
        assertThat(body.get("name").asText()).isEqualTo("田中");
        assertThat(body.get("departmentName").asText()).isEqualTo("開発部");
        assertDto(body);
    }

    @Test void nameIsOptionalAndMatchesPartOfName() throws Exception {
        JsonNode body = list("/employees?name=" + encode("田"), 2);
        assertThat(body.get(0).get("name").asText()).isEqualTo("田中");
        assertThat(body.get(1).get("name").asText()).isEqualTo("山田");
    }

    @Test void departmentMatchesExactName() throws Exception {
        JsonNode body = list("/employees?department=" + encode("営業部"), 2);
        for (JsonNode employee : body) {
            assertThat(employee.get("departmentName").asText()).isEqualTo("営業部");
        }
        list("/employees?department=" + encode("営業"), 0);
    }

    @Test void bothConditionsUseAnd() throws Exception {
        JsonNode body = list("/employees?name=" + encode("田")
                + "&department=" + encode("開発部"), 2);
        for (JsonNode employee : body) {
            assertDto(employee);
            assertThat(employee.get("departmentName").asText()).isEqualTo("開発部");
        }
        list("/employees?name=" + encode("田") + "&department=" + encode("営業部"), 0);
    }

    @Test void unmatchedConditionsReturnEmptyArrayWith200() throws Exception {
        list("/employees?name=" + encode("該当なし"), 0);
        list("/employees?department=" + encode("未知の部署"), 0);
    }

    @Test void blankQueriesAreTreatedAsAbsent() throws Exception {
        list("/employees?name=&department=" + encode(" 　"), 6);
    }

    @Test void surroundingWhitespaceIsRemoved() throws Exception {
        JsonNode body = list("/employees?name=" + encode(" 田中　")
                + "&department=" + encode(" 開発部 "), 1);
        assertThat(body.get(0).get("id").asInt()).isEqualTo(1);
    }

    @Test void unknownIdReturns404() throws Exception {
        assertThat(get("/employees/999").statusCode()).isEqualTo(404);
    }

    @Test void nonNumericIdReturns400() throws Exception {
        assertThat(get("/employees/abc").statusCode()).isEqualTo(400);
    }

    @Test void onlyDay2ControllerIsActive() {
        assertThat(context.getBeansOfType(Week4Day1EmployeeController.class)).isEmpty();
        assertThat(context.getBeansOfType(Week4Day2EmployeeController.class)).hasSize(1);
    }

    private JsonNode list(String path, int expectedSize) throws Exception {
        var response = get(path);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        JsonNode body = json.readTree(response.body());
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isEqualTo(expectedSize);
        return body;
    }

    private static void assertDto(JsonNode employee) {
        assertThat(employee.size()).isEqualTo(3);
        assertThat(employee.has("id")).isTrue();
        assertThat(employee.has("name")).isTrue();
        assertThat(employee.has("departmentName")).isTrue();
        assertThat(employee.has("department")).isFalse();
    }

    private HttpResponse<String> get(String path) throws Exception {
        URI uri = URI.create("http://localhost:"
                + environment.getRequiredProperty("local.server.port") + path);
        return client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
