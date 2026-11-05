package com.example.training.webmvc;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import tools.jackson.databind.*;

/** SPR-W6-D1-02: 同じ採番IDと内容をファイルへ記録し、実アプリ終了・再起動後に実HTTPで照合する。 */
public class Week6Day1HttpClient {
    private static final ObjectMapper JSON=new ObjectMapper();
    public static void main(String[] args) throws Exception {
        String mode=args[0]; Path marker=Path.of("data/spr-w6-d1-restart-marker.json");
        System.out.println("=== SPR-W6-D1 / "+mode+" / actual HTTP ===");
        if(mode.equals("post-before")) {
            var created=send("POST","/employees","{\"name\":\"W6再起動確認社員\",\"departmentId\":1}",201);
            Files.writeString(marker,created.toString(),StandardCharsets.UTF_8);
            if(created.get("id").asLong()<=0)throw new IllegalStateException("invalid generated id");
            System.out.println("Saved restart marker: "+marker.toAbsolutePath());
        } else if(mode.equals("get-after")) {
            var expected=JSON.readTree(Files.readString(marker,StandardCharsets.UTF_8));
            var actual=send("GET","/employees/"+expected.get("id").asLong(),null,200);
            if(!actual.equals(expected))throw new IllegalStateException("Restart data mismatch");
            System.out.println("Same ID, name and department verified after full restart.");
        } else if(mode.equals("initial-data")) {
            var rows=send("GET","/employees?name=W6%E9%96%8B%E7%99%BA%E7%94%A8",null,200);
            if(rows.size()!=2)throw new IllegalStateException("Expected two dev samples");
        } else throw new IllegalArgumentException(mode);
        System.out.println("PASS: "+mode+" (actual responses verified)");
    }
    private static JsonNode send(String method,String path,String body,int status)throws Exception {
        String url="http://localhost:8080"+path;
        var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15));
        if(body!=null)request.header("Content-Type","application/json");
        var response=HttpClient.newHttpClient().send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():
            HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println(method+" "+url);
        if(body!=null)System.out.println("Request: "+body);
        System.out.println("HTTP "+response.statusCode());
        System.out.println("Response: "+response.body());
        if(response.statusCode()!=status)throw new IllegalStateException("Unexpected HTTP response");
        return JSON.readTree(response.body());
    }
}
