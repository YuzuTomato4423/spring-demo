package com.example.training.webmvc;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Eclipse Consoleに実HTTPのURL・Status・DTO形式JSONを表示するRESTクライアント。 */
public class Week4Day2HttpClient {
    private record Scenario(String key, String label, String path, int status, int count) { }

    public static void main(String[] args) throws Exception {
        List<Scenario> cases = List.of(
                new Scenario("list", "SPR-W4-D2-01 / all employees", "/employees", 200, 6),
                new Scenario("detail", "SPR-W4-D2-02 / employee id=1", "/employees/1", 200, -1),
                new Scenario("name", "SPR-W4-D2-03 / name contains 田", "/employees?name=" + encode("田"), 200, 2),
                new Scenario("department", "SPR-W4-D2-03 / department=営業部",
                        "/employees?department=" + encode("営業部"), 200, 2),
                new Scenario("combined", "SPR-W4-D2-03 / name=田 AND department=開発部",
                        "/employees?name=" + encode("田") + "&department=" + encode("開発部"), 200, 2),
                new Scenario("no-match", "SPR-W4-D2-03 / no matching employee",
                        "/employees?name=" + encode("田") + "&department=" + encode("営業部"), 200, 0),
                new Scenario("dto", "SPR-W4-D2-04 / EmployeeResponse fields", "/employees/6", 200, -1),
                new Scenario("missing", "D2 / employee not found", "/employees/999", 404, -1),
                new Scenario("invalid", "D2 / non-numeric employee id", "/employees/abc", 400, -1));
        String selection = args.length == 0 ? "all" : args[0];
        if (!selection.equals("all") && cases.stream().noneMatch(item -> item.key().equals(selection))) {
            throw new IllegalArgumentException("Unknown scenario: " + selection);
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        ObjectMapper json = new ObjectMapper();
        System.out.println("=== D2 REST Client / live HTTP / carry-demo: 6 stored employees ===");
        for (Scenario item : cases) {
            if (!selection.equals("all") && !selection.equals(item.key())) {
                continue;
            }
            String url = "http://localhost:8080" + item.path();
            var response = client.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("\n=== " + item.label() + " ===");
            System.out.println("GET " + url);
            System.out.println("HTTP " + response.statusCode());
            System.out.println("Content-Type: " + response.headers().firstValue("content-type").orElse("(none)"));
            if (response.statusCode() != item.status()) {
                throw new IllegalStateException("Unexpected HTTP status: " + response.statusCode());
            }
            if (!response.body().isEmpty()) {
                JsonNode body = json.readTree(response.body());
                // JSON配列の各要素を1行に表示し、一覧と公開項目を同じConsoleで確認する。
                if (body.isArray()) {
                    System.out.println("[");
                    for (int i = 0; i < body.size(); i++) {
                        verifyDto(body.get(i));
                        System.out.println("  " + json.writeValueAsString(body.get(i))
                                + (i + 1 < body.size() ? "," : ""));
                    }
                    System.out.println("]");
                    if (body.size() != item.count()) {
                        throw new IllegalStateException("Unexpected result count: " + body.size());
                    }
                    System.out.println("Result count: " + body.size());
                } else {
                    System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(body));
                    if (item.status() == 200) {
                        verifyDto(body);
                    }
                }
            }
            System.out.println("PASS: actual HTTP status / result / DTO verification");
        }
        System.out.println("\nD2 REST Client completed successfully.");
    }

    private static void verifyDto(JsonNode employee) {
        if (employee.size() != 3 || !employee.has("id") || !employee.has("name")
                || !employee.has("departmentName") || employee.has("department")) {
            throw new IllegalStateException("Unexpected EmployeeResponse fields: " + employee);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
