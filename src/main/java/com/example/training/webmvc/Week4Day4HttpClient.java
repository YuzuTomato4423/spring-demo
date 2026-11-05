package com.example.training.webmvc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** D4の実HTTP応答をEclipse Consoleへ表示する。固定の成功表示ではなく受信結果を検証する。 */
public class Week4Day4HttpClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    public Week4Day4HttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "all" : args[0];
        if (!Set.of("all", "validation", "not-found", "common").contains(scenario)) {
            throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
        new Week4Day4HttpClient(args.length > 1 ? args[1] : "http://localhost:8080").run(scenario);
    }

    public void run(String scenario) throws Exception {
        JsonNode before = json.readTree(send("GET", "/employees", null).body());
        if (scenario.equals("all") || scenario.equals("validation")) {
            System.out.println("=== SPR-W4-D4-01 / Bean Validation / live HTTP ===");
            JsonNode error = showError("POST", "/employees", "{\"name\":\"\",\"departmentId\":0}", 400);
            if (error.get("errors").size() != 2 || !error.get("errors").has("name")
                    || !error.get("errors").has("departmentId")) throw new IllegalStateException("Missing field errors");
            System.out.println("PASS: @NotBlank + @Positive / HTTP 400 / field errors");
        }
        if (scenario.equals("all") || scenario.equals("not-found")) {
            System.out.println("=== SPR-W4-D4-02 / EmployeeNotFoundException / live HTTP ===");
            showError("GET", "/employees/999999", null, 404);
            System.out.println("PASS: Service not-found -> Advice -> HTTP 404");
        }
        if (scenario.equals("all") || scenario.equals("common")) {
            System.out.println("=== SPR-W4-D4-03 / common error JSON / live HTTP ===");
            JsonNode bad = showError("POST", "/employees", "{\"name\":\"\",\"departmentId\":1}", 400);
            JsonNode missing = showError("GET", "/employees/999999", null, 404);
            if (bad.size() != missing.size()) throw new IllegalStateException("Different error structures");
            System.out.println("PASS: HTTP 400 / 404 -> status, message, errors");
        }
        JsonNode after = json.readTree(send("GET", "/employees", null).body());
        if (!before.equals(after)) throw new IllegalStateException("Existing employees changed");
        System.out.println("Existing employees unchanged: " + after.size());
    }

    private JsonNode showError(String method, String path, String body, int expected) throws Exception {
        var response = send(method, path, body);
        System.out.println(method + " " + baseUrl + path);
        if (body != null) System.out.println("Request: " + body);
        System.out.println("HTTP " + response.statusCode());
        System.out.println("Content-Type: " + response.headers().firstValue("content-type").orElse(""));
        JsonNode error = json.readTree(response.body());
        System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(error));
        if (response.statusCode() != expected || error.size() != 3
                || !error.has("status") || error.get("status").asInt() != expected
                || !error.has("message") || error.get("message").asText().isBlank()
                || !error.has("errors") || !error.get("errors").isObject()) {
            throw new IllegalStateException("Unexpected HTTP error response");
        }
        return error;
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(5));
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
