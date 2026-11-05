package com.example.training.webmvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Eclipse Consoleへ実HTTPの入力・応答・更新前後を表示する。検証失敗は例外で終了する。 */
public class Week4Day3HttpClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    public Week4Day3HttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static void main(String[] args) throws Exception {
        String selection = args.length == 0 ? "all" : args[0];
        if (!java.util.Set.of("all", "post", "put", "delete", "summary").contains(selection)) {
            throw new IllegalArgumentException("Unknown scenario: " + selection);
        }
        new Week4Day3HttpClient(args.length > 1 ? args[1] : "http://localhost:8080").run(selection);
    }

    public void run(String selection) throws Exception {
        System.out.println("=== SPR-W4-D3 / live HTTP / " + baseUrl + " ===");
        // シナリオごとに専用社員を作り、既存の6名を更新・削除しない。
        String original = "{\"name\":\"D3登録社員\",\"departmentId\":1}";
        boolean showPost = selection.equals("all") || selection.equals("post");
        var post = send("POST", "/employees", original, 201, showPost);
        JsonNode created = json.readTree(post.body());
        verifyDto(created, "D3登録社員", "開発部");
        int id = created.get("id").asInt();
        String path = "/employees/" + id;
        if (!post.headers().firstValue("location").orElse("").equals(path)) {
            throw new IllegalStateException("Location mismatch");
        }
        System.out.println("Created employee id=" + id + " (dedicated test data)");
        if (selection.equals("post")) {
            verifyDto(json.readTree(send("GET", path, null, 200, true).body()), "D3登録社員", "開発部");
            send("DELETE", path, null, 204, false);
            System.out.println("PASS: POST / 201 / Location / DTO / GET verification");
            return;
        }

        boolean showPut = selection.equals("all") || selection.equals("put");
        if (showPut) System.out.println("\n=== SPR-W4-D3-02 / BEFORE ===");
        verifyDto(json.readTree(send("GET", path, null, 200, showPut).body()), "D3登録社員", "開発部");
        if (showPut) System.out.println("\n=== SPR-W4-D3-02 / UPDATE ===");
        String updated = "{\"name\":\"D3更新社員\",\"departmentId\":2}";
        var put = send("PUT", path, updated, 200, showPut);
        verifyDto(json.readTree(put.body()), "D3更新社員", "営業部");
        if (showPut) System.out.println("\n=== SPR-W4-D3-02 / AFTER ===");
        verifyDto(json.readTree(send("GET", path, null, 200, showPut).body()), "D3更新社員", "営業部");

        boolean showDelete = selection.equals("all") || selection.equals("delete");
        if (showDelete) System.out.println("\n=== SPR-W4-D3-03 / DELETE ===");
        var deleted = send("DELETE", path, null, 204, showDelete);
        if (!deleted.body().isEmpty()) throw new IllegalStateException("204 body must be empty");
        send("GET", path, null, 404, showDelete);
        if (showDelete) System.out.println("PASS: DELETE / 204 / empty body / GET 404 verification");
        if (showPut) System.out.println("PASS: PUT / 200 / same id / updated name and department");

        System.out.println("\n=== SPR-W4-D3-04 / ACTUAL HTTP STATUS SUMMARY ===");
        System.out.println("METHOD  PATH                  EXPECTED  ACTUAL  RESULT");
        System.out.printf("POST    %-20s 201       %d     PASS%n", "/employees", post.statusCode());
        System.out.printf("PUT     %-20s 200       %d     PASS%n", path, put.statusCode());
        System.out.printf("DELETE  %-20s 204       %d     PASS%n", path, deleted.statusCode());
        System.out.println("GET after DELETE: 404; DELETE response body: empty");
        System.out.println("D3 REST Client completed successfully.");
    }

    private HttpResponse<String> send(String method, String path, String body, int expected, boolean display)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(5));
        if (body != null) builder.header("Content-Type", "application/json");
        var response = client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (display) {
            System.out.println(method + " " + baseUrl + path);
            if (body != null) System.out.println("Request: " + body);
            System.out.println("HTTP " + response.statusCode());
            response.headers().firstValue("location").ifPresent(value -> System.out.println("Location: " + value));
            System.out.println("Response: " + (response.body().isEmpty() ? "(empty)" : response.body()));
        }
        if (response.statusCode() != expected) {
            throw new IllegalStateException(method + " " + path + ": expected " + expected
                    + " but received " + response.statusCode());
        }
        return response;
    }

    private static void verifyDto(JsonNode body, String name, String department) {
        if (body.size() != 3 || !body.has("id") || !body.has("name") || !body.has("departmentName")
                || !body.get("name").asText().equals(name)
                || !body.get("departmentName").asText().equals(department)) {
            throw new IllegalStateException("EmployeeResponse mismatch: " + body);
        }
    }
}
