package com.example.demo.week7.day2;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * SPR-W7-D2-01: 実HTTP→Controller→Validation/Advice→Service→Repository→H2を統合する。
 * SPR-W7-D2-03: HTTPのService Transactionはサーバ側で完了するため、テスト全体を
 * Transactionで囲んでもHTTP更新はrollbackされない。各テスト前後に専用DBの社員だけを
 * 初期化し、独立JDBC接続からcommit済み状態を確認する。部署の初期データ2件は保持する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"spr-w6-d3", "test", "spr-w7-d2-test"})
@Tag("spr-w7-d2")
abstract class Week7Day2IntegrationSupport {
    @Autowired ConfigurableApplicationContext context;
    @Autowired Environment environment;
    @Autowired JdbcTemplate jdbc;
    final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    final ObjectMapper json = new ObjectMapper();
    private boolean verifiedIsolatedDatabase;

    String actualJdbcUrl() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            return connection.getMetaData().getURL();
        }
    }

    @BeforeEach
    void initializeOnlyVerifiedTestDatabase(TestInfo test) throws Exception {
        verifiedIsolatedDatabase = false;
        assertThat(environment.getActiveProfiles()).contains("test", "spr-w7-d2-test")
                .doesNotContain("dev", "prod", "spr-w6-d5", "spr-w6-d1");
        assertThat(actualJdbcUrl()).startsWith("jdbc:h2:mem:sprw7d2-test-");
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        verifiedIsolatedDatabase = true;
        jdbc.update("DELETE FROM EMPLOYEE");
        assertThat(employeeCount()).isZero();
        assertThat(departmentCount()).isEqualTo(2);
        System.out.printf("[SPR-W7-D2] START %s | EMPLOYEE=0 DEPARTMENT=2%n", test.getDisplayName());
    }

    @AfterEach
    void removeCommittedHttpData(TestInfo test) throws Exception {
        if (!verifiedIsolatedDatabase) return;
        assertThat(actualJdbcUrl()).startsWith("jdbc:h2:mem:sprw7d2-test-");
        jdbc.update("DELETE FROM EMPLOYEE");
        assertThat(employeeCount()).isZero();
        assertThat(departmentCount()).isEqualTo(2);
        System.out.printf("[SPR-W7-D2] CLEAN %s | EMPLOYEE=0 DEPARTMENT=2%n", test.getDisplayName());
    }

    int employeeCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE", Integer.class); }
    int departmentCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM DEPARTMENT", Integer.class); }

    String employee(String name, String email, int departmentId) {
        return json.writeValueAsString(Map.of("name", name, "email", email, "departmentId", departmentId));
    }

    HttpResponse<String> send(String method, String path, String body, int status) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getProperty("local.server.port") + path)).timeout(Duration.ofSeconds(10));
        if (body != null) builder.header("Content-Type", "application/json");
        var request = builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("%s %s: %s", method, path, response.body()).isEqualTo(status);
        System.out.printf("[SPR-W7-D2] HTTP %s %s -> %d%n", method, path, response.statusCode());
        return response;
    }

    JsonNode tree(HttpResponse<String> response) { return json.readTree(response.body()); }

    void assertCommittedEmployee(long id, String name, String email, int departmentId) {
        var row = jdbc.queryForMap("SELECT ID, NAME, EMAIL, DEPARTMENT_ID FROM EMPLOYEE WHERE ID=?", id);
        assertThat(((Number) row.get("ID")).longValue()).isEqualTo(id);
        assertThat(row.get("NAME")).isEqualTo(name);
        assertThat(row.get("EMAIL")).isEqualTo(email);
        assertThat(((Number) row.get("DEPARTMENT_ID")).intValue()).isEqualTo(departmentId);
        assertThat(employeeCount()).isEqualTo(1);
        System.out.printf("[SPR-W7-D2] JDBC committed | ID=%d NAME=%s EMAIL=%s DEPARTMENT_ID=%d ROWS=1%n",
                id, name, email, departmentId);
    }
}
