package com.example.training.webmvc;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;

/** 課題証跡のため、Eclipse Consoleに実HTTPの要求と応答を表示する。 */
public class Week5Day3HttpClient {
    private static final ObjectMapper JSON=new ObjectMapper();
    public static void main(String[] args) throws Exception {
        String scenario=args.length==0?"list":args[0];
        if(scenario.equals("setup")) {
            for(var data:new String[][]{{"田中太郎","1"},{"田中太郎","2"},{"佐藤花子","1"}}) {
                send("POST","/employees",JSON.writeValueAsString(java.util.Map.of(
                        "name",data[0],"departmentId",Integer.parseInt(data[1]))),201);
            }
        } else {
            String path=switch(scenario) {
                case "list" -> "/employees";
                case "name" -> "/employees?name="+encode("田中");
                case "combined" -> "/employees?name="+encode("田中")+"&department="+encode("開発部");
                default -> throw new IllegalArgumentException("Unknown scenario: "+scenario);
            };
            send("GET",path,null,200);
        }
    }
    private static String encode(String v) { return URLEncoder.encode(v,StandardCharsets.UTF_8); }
    private static void send(String method,String path,String body,int expected) throws Exception {
        var url="http://localhost:8080"+path;
        var request=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(10));
        if(body!=null) request.header("Content-Type","application/json");
        var response=HttpClient.newHttpClient().send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():
                HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("=== SPR-W5-D3 / actual HTTP / "+java.time.ZonedDateTime.now()+" ===");
        System.out.println(method+" "+URLDecoder.decode(url,StandardCharsets.UTF_8));
        System.out.println("HTTP "+response.statusCode());
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree(response.body())));
        if(response.statusCode()!=expected) throw new IllegalStateException("Unexpected status");
        System.out.println("PASS: actual HTTP status = "+expected);
    }
}
