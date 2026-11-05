package com.example.training.webmvc;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import tools.jackson.databind.ObjectMapper;

/** Eclipse Consoleに実HTTP結果を表示する。POSTの社員はH2照合のため残す。 */
public class Week5Day2HttpClient {
    public static void main(String[] args) throws Exception {
        String scenario = args.length == 0 ? "list" : args[0];
        String method, path, body = null;
        int expected;
        switch (scenario) {
            case "post" -> { method="POST"; path="/employees";
                body="{\"name\":\"D2永続化確認社員\",\"departmentId\":1}"; expected=201; }
            case "list" -> { method="GET"; path="/employees"; expected=200; }
            case "detail", "missing", "delete" -> {
                if(args.length != 2) throw new IllegalArgumentException("Employee ID is required");
                long id=Long.parseLong(args[1]);
                if(id<=0) throw new IllegalArgumentException("Positive employee ID is required");
                path="/employees/"+id;
                method=scenario.equals("delete") ? "DELETE" : "GET";
                expected=scenario.equals("delete") ? 204 : scenario.equals("missing") ? 404 : 200;
            }
            default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
        String url="http://localhost:8080"+path;
        var builder=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
        if(body!=null) builder.header("Content-Type","application/json");
        var response=HttpClient.newHttpClient().send(builder.method(method,body==null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("=== SPR-W5-D2 / actual HTTP / " + java.time.ZonedDateTime.now() + " ===");
        System.out.println(method+" "+url);
        if(body!=null) System.out.println("Request: "+body);
        System.out.println("HTTP "+response.statusCode());
        response.headers().firstValue("location").ifPresent(v->System.out.println("Location: "+v));
        System.out.println("Response:");
        System.out.println(response.body().isEmpty() ? "(empty)" :
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                        new ObjectMapper().readTree(response.body())));
        if(response.statusCode()!=expected) throw new IllegalStateException("Expected HTTP "+expected);
        if(expected==204 && !response.body().isEmpty()) throw new IllegalStateException("204 must be empty");
        System.out.println("PASS: actual HTTP status = " + expected);
    }
}
