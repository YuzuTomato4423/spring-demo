package com.example.training.webmvc;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import tools.jackson.databind.ObjectMapper;

/** EclipseのConsoleで実HTTPのURL・ステータス・本文を確認する学習用RESTクライアント。 */
public class Week4Day1HttpClient {
    public static void main(String[] args) throws Exception {
        String base = "http://localhost:8080";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        ObjectMapper json = new ObjectMapper();
        String[][] cases = {
            {"SPR-W4-D1-02 / hello", "/hello"},
            {"SPR-W4-D1-03 / id=1", "/employees/1"},
            {"SPR-W4-D1-03 / id=2", "/employees/2"},
            {"SPR-W4-D1-04 / department=開発部 (name省略)", "/employees?department=" + encode("開発部")},
            {"SPR-W4-D1-04 / department=営業部 (name省略)", "/employees?department=" + encode("営業部")},
            {"SPR-W4-D1-04 / department=開発部 & name=田中", "/employees?department=" + encode("開発部") + "&name=" + encode("田中")}
        };
        System.out.println("=== D1 REST Client: live HTTP from Eclipse ===");
        for (String[] item : cases) {
            String url = base + item[1];
            var response = client.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("\n=== " + item[0] + " ===");
            System.out.println("GET " + url);
            System.out.println("HTTP " + response.statusCode());
            if (response.headers().firstValue("content-type").orElse("").contains("application/json")) {
                System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(json.readTree(response.body())));
            } else {
                System.out.println(response.body());
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Unexpected HTTP status for " + url);
            }
        }
        System.out.println("\nAll 6 live GET requests returned HTTP 200.");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
