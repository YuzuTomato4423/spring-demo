package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.service.EmployeeJpaService;
import com.example.demo.service.EmployeeService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** 実HTTPと別JDBC取得を照合し、JPAのキャッシュだけで成功しないことを確認する。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:sprw5d2-test-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
    "spring.h2.console.enabled=false"
})
@ActiveProfiles({"spr-w5-d1", "spr-w5-d2", "carry-demo"})
class Week5Day2JpaHttpTests {
    @Autowired Environment environment;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmployeeRegistrationNotifier notifier;
    @Autowired EmployeeJpaService service;
    @Autowired EmployeeService legacyService;
    @Autowired PlatformTransactionManager transactions;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach void cleanIsolatedDatabase() {
        jdbc.update("DELETE FROM EMPLOYEE");
        jdbc.execute("ALTER TABLE EMPLOYEE ALTER COLUMN ID RESTART WITH 1");
    }

    @Test void postGetPutDeleteUseSameCommittedDatabaseAndDtoContract() throws Exception {
        int delivered = notifier.getDeliveryCount();
        var created = send("POST", "/employees", body("永続化社員", 1), 201);
        JsonNode employee = json.readTree(created.body());
        long id = employee.get("id").asLong();
        assertThat(id).isPositive();
        assertThat(employee.size()).isEqualTo(3);
        assertThat(employee.get("name").asText()).isEqualTo("永続化社員");
        assertThat(employee.get("departmentName").asText()).isEqualTo("開発部");
        assertThat(created.headers().firstValue("location")).contains("/employees/" + id);
        assertThat(jdbc.queryForObject("SELECT NAME FROM EMPLOYEE WHERE ID=?", String.class, id))
                .isEqualTo("永続化社員");
        assertThat(notifier.getDeliveryCount()).isEqualTo(delivered + 1);
        assertThat(json.readTree(send("GET", "/employees/" + id, null, 200).body())).isEqualTo(employee);
        assertThat(json.readTree(send("GET", "/employees", null, 200).body()).get(0)).isEqualTo(employee);

        JsonNode second = json.readTree(send("POST", "/employees", body("別社員", 2), 201).body());
        assertThat(second.get("id").asLong()).isGreaterThan(id);
        JsonNode updated = json.readTree(send("PUT", "/employees/" + id, body("更新社員", 2), 200).body());
        assertThat(updated.get("id").asLong()).isEqualTo(id);
        assertThat(updated.get("departmentName").asText()).isEqualTo("営業部");
        assertThat(jdbc.queryForObject("SELECT NAME FROM EMPLOYEE WHERE ID=?", String.class, id))
                .isEqualTo("更新社員");
        assertThat(jdbc.queryForObject("SELECT DEPARTMENT_ID FROM EMPLOYEE WHERE ID=?", Integer.class, id))
                .isEqualTo(2);
        assertThat(count()).isEqualTo(2);
        assertThat(json.readTree(send("GET", "/employees/" + id, null, 200).body())).isEqualTo(updated);
        assertThat(send("DELETE", "/employees/" + id, null, 204).body()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE WHERE ID=?", Integer.class, id)).isZero();
        error(send("GET", "/employees/" + id, null, 404), 404);
        error(send("DELETE", "/employees/" + id, null, 404), 404);
        assertThat(count()).isEqualTo(1);
        assertThat(notifier.getDeliveryCount()).isEqualTo(delivered + 2);
        // carry-demoを同時指定しても旧サンプル登録が起動せず、APIは旧保存先へ書かない。
        assertThat(legacyService.findAll()).isEmpty();
    }

    @Test void invalidInputsDoNotWriteOrNotifyAndKeepCommonErrorFormat() throws Exception {
        int delivered = notifier.getDeliveryCount();
        for (String payload : new String[]{body("", 1), body("   ", 1), body("社員", 0),
                body("社員", 999), "{", "null", "", "{\"name\":null,\"departmentId\":1}",
                "{\"name\":\"社員\"}", "{\"name\":\"社員\",\"departmentId\":\"abc\"}"}) {
            error(send("POST", "/employees", payload, 400), 400);
            error(send("PUT", "/employees/1", payload, payload.contains("999") ? 404 : 400),
                    payload.contains("999") ? 404 : 400);
        }
        assertThat(count()).isZero();
        assertThat(notifier.getDeliveryCount()).isEqualTo(delivered);
        assertThatThrownBy(() -> service.create("", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("社員", 999)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void missingAndMalformedIdsReturn404And400WithoutMutation() throws Exception {
        for (String method : new String[]{"GET", "PUT", "DELETE"}) {
            String payload = method.equals("PUT") ? body("社員", 1) : null;
            for (String id : new String[]{"999999", "9223372036854775807", "-1"}) {
                JsonNode result = error(send(method, "/employees/" + id, payload, 404), 404);
                assertThat(result.get("message").asText()).isEqualTo("社員が見つかりません。 id=" + id);
            }
            error(send(method, "/employees/abc", payload, 400), 400);
            error(send(method, "/employees/9223372036854775808", payload, 400), 400);
        }
        assertThat(count()).isZero();
    }

    @Test void existingSearchRulesAndLongIdsArePreserved() throws Exception {
        jdbc.execute("ALTER TABLE EMPLOYEE ALTER COLUMN ID RESTART WITH 2147483650");
        long id = json.readTree(send("POST", "/employees", body("Long確認社員", 1), 201).body()).get("id").asLong();
        assertThat(id).isGreaterThan(Integer.MAX_VALUE);
        assertThat(notifier.getLastResult()).contains(Long.toString(id));
        send("POST", "/employees", body("別部署社員", 2), 201);
        String query = "?name=" + encode(" 確認 ") + "&department=" + encode(" 開発部 ");
        JsonNode results = json.readTree(send("GET", "/employees" + query, null, 200).body());
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).get("id").asLong()).isEqualTo(id);
        assertThat(json.readTree(send("GET", "/employees?name=no-match", null, 200).body()).size()).isZero();
        assertThat(json.readTree(send("GET", "/employees?name=%20&department=%20", null, 200).body()).size()).isEqualTo(2);
        send("GET", "/employees/" + id, null, 200);
        send("DELETE", "/employees/" + id, null, 204);
        send("GET", "/employees/" + id, null, 404);
    }

    @Test void rolledBackRegistrationDoesNotPersistOrNotify() {
        int delivered = notifier.getDeliveryCount();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.create("ロールバック社員", 1);
            status.setRollbackOnly();
        });
        assertThat(count()).isZero();
        assertThat(notifier.getDeliveryCount()).isEqualTo(delivered);
    }

    private int count() { return jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE", Integer.class); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String body(String name, int department) {
        return json.writeValueAsString(java.util.Map.of("name", name, "departmentId", department));
    }
    private JsonNode error(HttpResponse<String> response, int status) {
        JsonNode value = json.readTree(response.body());
        assertThat(value.get("status").asInt()).isEqualTo(status);
        assertThat(value.get("message").asText()).isNotBlank();
        assertThat(value.get("errors").isObject()).isTrue();
        return value;
    }
    private HttpResponse<String> send(String method, String path, String body, int status) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + environment.getProperty("local.server.port") + path))
                .timeout(Duration.ofSeconds(10));
        if (body != null) request.header("Content-Type", "application/json");
        var response = client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as("%s %s: %s", method, path, response.body()).isEqualTo(status);
        return response;
    }
}
