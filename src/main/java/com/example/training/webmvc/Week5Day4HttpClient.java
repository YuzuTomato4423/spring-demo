package com.example.training.webmvc;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import tools.jackson.databind.*;

/** SPR-W5-D4: Eclipse Consoleへ実際の要求・HTTP status・JSONを表示する再実行用クライアント。 */
public class Week5Day4HttpClient {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final HttpClient CLIENT=HttpClient.newHttpClient();
    public static void main(String[] args) throws Exception {
        String scenario=args.length==0?"page-0":args[0];
        System.out.println("=== SPR-W5-D4 / "+scenario+" / actual HTTP / "+java.time.ZonedDateTime.now()+" ===");
        if(scenario.equals("setup")) {
            if(!JSON.readTree(send("GET","/employees",null,200)).isEmpty())
                throw new IllegalStateException("Setup requires empty employee DB; restart dedicated launch first");
            for(var row:new String[][]{{"Charlie","1"},{"Alpha","1"},{"Bravo","2"},{"Alpha","2"}})
                send("POST","/employees",body(row[0],Integer.parseInt(row[1])),201);
        } else if(scenario.equals("put")) {
            long id=firstId(); send("PUT","/employees/"+id,body("Delta",2),200);
        } else if(scenario.equals("rollback")) {
            long id=firstId(); send("POST","/training/rollback/employees/"+id,body("ROLLBACK_TEMP",1),500);
            send("GET","/employees/"+id,null,200);
        } else {
            String query=switch(scenario) {
                case "page-0" -> "?page=0&size=2&sort=id,asc";
                case "page-1" -> "?page=1&size=2&sort=id,asc";
                case "sort-name-asc" -> "?sort=name,asc";
                case "sort-name-desc" -> "?sort=name,desc";
                case "sort-id-asc" -> "?sort=id,asc";
                case "sort-id-desc" -> "?sort=id,desc";
                default -> throw new IllegalArgumentException("Unknown scenario: "+scenario);
            };
            send("GET","/employees"+query,null,200);
        }
    }
    private static long firstId() throws Exception {
        var employees=JSON.readTree(send("GET","/employees?sort=id,asc",null,200));
        if(employees.isEmpty()) throw new IllegalStateException("Run setup first");
        return employees.get(0).get("id").asLong();
    }
    private static String body(String name,int department) throws Exception {
        return JSON.writeValueAsString(Map.of("name",name,"departmentId",department));
    }
    private static String send(String method,String path,String body,int expected) throws Exception {
        String url="http://localhost:8080"+path;
        var request=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(10));
        if(body!=null) request.header("Content-Type","application/json");
        var response=CLIENT.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():
                HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println(method+" "+url);
        if(body!=null) System.out.println("Request JSON: "+body);
        System.out.println("HTTP "+response.statusCode());
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree(response.body())));
        if(response.statusCode()!=expected) throw new IllegalStateException("Unexpected HTTP status");
        return response.body();
    }
}
