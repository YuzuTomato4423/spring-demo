package com.example.training.webmvc;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.*;

/** SPR-W5-D5-01～03: 実HTTPの要求・応答・検証結果をEclipse Consoleへ表示する。 */
public class Week5Day5HttpClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String CREATED = "D5統合社員_作成", UPDATED = "D5統合社員_更新";
    private static final String KEPT = "D5異常系_保持", RESTART = "D5再起動確認";

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "create-read" : args[0];
        System.out.println("=== SPR-W5-D5 / " + scenario + " / actual HTTP ===");
        System.out.println(java.time.ZonedDateTime.now());
        switch (scenario) {
            case "create-read" -> {
                empty();
                var created = send("POST", "/employees", body(CREATED, 1), 201);
                long id = created.json().get("id").asLong();
                require(id > 0 && created.location().equals("/employees/" + id), "generated ID / Location");
                require(send("GET", "/employees/" + id, null, 200).json().equals(created.json()), "POST / GET same DTO");
            }
            case "update-search" -> {
                long id = find(CREATED);
                var updated = send("PUT", "/employees/" + id, body(UPDATED, 2), 200).json();
                require(updated.get("id").asLong() == id, "same employee ID");
                require(updated.get("departmentName").asText().equals("営業部"), "updated department");
                var found = send("GET", "/employees?name=" + encode("統合社員") + "&department=" + encode("営業部"), null, 200).json();
                require(found.size() == 1 && found.get(0).equals(updated), "combined search finds updated employee");
                require(send("GET", "/employees?name=" + encode(CREATED), null, 200).json().isEmpty(), "old name no longer matches");
            }
            case "delete" -> {
                long id = find(UPDATED);
                send("DELETE", "/employees/" + id, null, 204);
                error(send("GET", "/employees/" + id, null, 404).json(), 404);
                empty();
            }
            case "errors-prepare" -> {
                empty();
                send("POST", "/employees", body(KEPT, 1), 201);
                send("GET", "/employees", null, 200);
            }
            case "validation" -> {
                long id = find(KEPT);
                var before = send("GET", "/employees", null, 200).json();
                var invalid = send("POST", "/employees", body("", 0), 400).json();
                error(invalid, 400);
                require(invalid.get("errors").has("name") && invalid.get("errors").has("departmentId"), "both field errors");
                error(send("PUT", "/employees/" + id, body("   ", 0), 400).json(), 400);
                error(send("PUT", "/employees/" + id, body("不正部署", 999), 400).json(), 400);
                require(send("GET", "/employees", null, 200).json().equals(before), "400 leaves employees unchanged");
            }
            case "not-found" -> {
                long missing = find(KEPT) + 1000000;
                var before = send("GET", "/employees", null, 200).json();
                for (String method : List.of("GET", "PUT", "DELETE"))
                    error(send(method, "/employees/" + missing, method.equals("PUT") ? body("未存在", 1) : null, 404).json(), 404);
                require(send("GET", "/employees", null, 200).json().equals(before), "404 leaves employees unchanged");
            }
            case "restart-before" -> {
                find(KEPT);
                send("POST", "/employees", body(RESTART, 2), 201);
                require(send("GET", "/employees", null, 200).json().size() == 2, "two employees before restart");
            }
            case "restart-after" -> empty();
            case "representative" -> {
                empty();
                var saved = send("POST", "/employees", body("D5最終確認", 1), 201).json();
                send("GET", "/employees/" + saved.get("id").asLong(), null, 200);
                send("GET", "/employees?name=" + encode("最終") + "&department=" + encode("開発部"), null, 200);
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
        System.out.println("PASS: " + scenario + " (actual responses verified)");
    }
    private static void empty() throws Exception {
        require(send("GET", "/employees", null, 200).json().isEmpty(), "dedicated employee DB is empty");
    }
    private static long find(String name) throws Exception {
        var list = send("GET", "/employees?name=" + encode(name), null, 200).json();
        require(list.size() == 1 && list.get(0).get("name").asText().equals(name), "unique scenario employee");
        return list.get(0).get("id").asLong();
    }
    private static void error(JsonNode node, int status) {
        require(node.size() == 3 && node.has("status") && node.has("message") && node.has("errors"), "common error keys");
        require(node.get("status").asInt() == status && !node.get("message").asText().isBlank() && node.get("errors").isObject(), "common error values");
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String body(String name, int departmentId) throws Exception {
        return JSON.writeValueAsString(Map.of("name", name, "departmentId", departmentId));
    }
    private static void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("FAIL: " + label);
    }
    private record Result(JsonNode json, String location) { }
    private static Result send(String method, String path, String body, int expected) throws Exception {
        String url = "http://localhost:8080" + path;
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
        if (body != null) request.header("Content-Type", "application/json");
        var response = HTTP.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() :
                HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println(method + " " + url);
        if (body != null) System.out.println("Request: " + body);
        String location = response.headers().firstValue("location").orElse("");
        System.out.println("HTTP " + response.statusCode() + (location.isEmpty() ? "" : " / Location: " + location));
        System.out.println("Response: " + (response.body().isEmpty() ? "<empty>" : response.body()));
        require(response.statusCode() == expected, "HTTP status " + expected);
        require(expected != 204 || response.body().isEmpty(), "204 has no response body");
        return new Result(response.body().isEmpty() ? null : JSON.readTree(response.body()), location);
    }
}
