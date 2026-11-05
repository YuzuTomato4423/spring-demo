package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.example.demo.exception.EmployeeNotFoundException;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.service.EmployeeService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 実HTTPでValidationと共通Adviceを通し、エラー形式と保存・通知の抑止を確認する。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.application.name=day4-http-tests")
@ActiveProfiles("carry-demo")
class Week4Day4HttpTests {
    @Autowired private Environment environment;
    @Autowired private EmployeeRegistrationNotifier notifier;
    @Autowired private EmployeeService service;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test void invalidNamesNeverReachSaveOrNotification() throws Exception {
        JsonNode before = employees();
        int notifications = notifier.getDeliveryCount();
        for (String body : new String[]{"{\"departmentId\":1}",
                "{\"name\":null,\"departmentId\":1}", body("", 1),
                body("   ", 1), body("　 ", 1), body("\\t\\n", 1)}) {
            for (String method : new String[]{"POST", "PUT"}) {
                JsonNode error = error(send(method, method.equals("POST") ? "/employees" : "/employees/1", body), 400);
                assertThat(error.get("errors").get("name").asText()).isEqualTo("社員名を入力してください。");
            }
        }
        assertThat(employees()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    @Test void invalidDepartmentsAndMultipleConstraintsShareTheErrorContract() throws Exception {
        JsonNode before = employees();
        int notifications = notifier.getDeliveryCount();
        for (String method : new String[]{"POST", "PUT"}) {
            error(send(method, method.equals("POST") ? "/employees" : "/employees/1", "{\"name\":\"社員\"}"), 400);
        }
        for (String body : new String[]{body("社員", 0), body("社員", -1)}) {
            for (String method : new String[]{"POST", "PUT"}) {
                JsonNode error = error(send(method, method.equals("POST") ? "/employees" : "/employees/1", body), 400);
                assertThat(error.get("errors").get("departmentId").asText()).isEqualTo("部署IDは正の整数で指定してください。");
            }
        }
        JsonNode multiple = error(send("POST", "/employees", body("", 0)), 400);
        assertThat(multiple.get("errors").size()).isEqualTo(2);
        for (String method : new String[]{"POST", "PUT"}) {
            JsonNode business = error(send(method, method.equals("POST") ? "/employees" : "/employees/1", body("社員", 999)), 400);
            assertThat(business.get("message").asText()).isEqualTo("存在しない部署です。");
            assertThat(business.get("errors").isEmpty()).isTrue();
        }
        assertThat(employees()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    @Test void absentEmployeeIs404ForGetPutAndDelete() throws Exception {
        JsonNode before = employees();
        int notifications = notifier.getDeliveryCount();
        for (String method : new String[]{"GET", "PUT", "DELETE"}) {
            JsonNode error = error(send(method, "/employees/999999", method.equals("PUT") ? body("社員", 1) : null), 404);
            assertThat(error.get("message").asText()).isEqualTo("社員が見つかりません。 id=999999");
            assertThat(error.get("errors").isEmpty()).isTrue();
        }
        assertThat(employees()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    @Test void unreadableMissingAndWrongTypeInputsAreCommon400Responses() throws Exception {
        JsonNode before = employees();
        int notifications = notifier.getDeliveryCount();
        for (String method : new String[]{"POST", "PUT"}) {
            String path = method.equals("POST") ? "/employees" : "/employees/1";
            for (String body : new String[]{"{", "null", "", "{\"name\":\"社員\",\"departmentId\":\"abc\"}"}) {
                JsonNode error = error(send(method, path, body), 400);
                assertThat(error.get("errors").isEmpty()).isTrue();
                assertThat(error.get("message").asText()).isEqualTo("JSON形式のリクエスト本文を指定してください。");
            }
            error(send(method, path, null), 400);
        }
        for (String method : new String[]{"GET", "PUT", "DELETE"}) {
            JsonNode error = error(send(method, "/employees/abc", method.equals("PUT") ? body("社員", 1) : null), 400);
            assertThat(error.get("message").asText()).isEqualTo("IDは整数で指定してください。");
        }
        assertThat(employees()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    @Test void serviceRetainsDomainValidationWithoutHttp() {
        int count = service.findAll().size();
        int notifications = notifier.getDeliveryCount();
        assertThatThrownBy(() -> service.findById(999999)).isInstanceOf(EmployeeNotFoundException.class);
        assertThatThrownBy(() -> service.update(999999, "社員", 1)).isInstanceOf(EmployeeNotFoundException.class);
        assertThatThrownBy(() -> service.delete(999999)).isInstanceOf(EmployeeNotFoundException.class);
        assertThatThrownBy(() -> service.create("", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("社員", 999)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.findAll()).hasSize(count);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    private JsonNode employees() throws Exception {
        var response = send("GET", "/employees", null);
        assertThat(response.statusCode()).isEqualTo(200);
        return json.readTree(response.body());
    }

    private JsonNode error(HttpResponse<String> response, int status) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        JsonNode error = json.readTree(response.body());
        assertThat(error.size()).isEqualTo(3);
        assertThat(error.get("status").asInt()).isEqualTo(status);
        assertThat(error.get("message").asText()).isNotBlank();
        assertThat(error.get("errors").isObject()).isTrue();
        return error;
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getRequiredProperty("local.server.port") + path)).timeout(Duration.ofSeconds(5));
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String body(String name, int departmentId) {
        return "{\"name\":\"" + name + "\",\"departmentId\":" + departmentId + "}";
    }
}
