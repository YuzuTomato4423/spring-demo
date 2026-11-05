package com.example.training.webmvc;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;
import tools.jackson.databind.*;

/** SPR-W6-D3: 実HTTPとH2 Consoleの独立JDBC接続による読取をEclipse Consoleへ表示する。 */
public class Week6Day3HttpClient {
    private static final HttpClient HTTP=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String BASE="http://localhost:8080";
    public static void main(String[] args) throws Exception {
        String scenario=args.length==0?"register":args[0];
        System.out.println("=== SPR-W6-D3 / "+scenario+" / actual HTTP ===");
        System.out.println(java.time.ZonedDateTime.now());
        switch(scenario) {
            case "register" -> send("POST","/employees",employee("D3社員A","  Alice@Example.TEST  ",1),201);
            case "duplicate" -> send("POST","/employees",employee("重複拒否","ALICE@EXAMPLE.TEST",2),409);
            case "department" -> {
                long id=find("D3社員A");
                send("PUT","/employees/"+id,employee("D3社員A","alice@example.test",2),200);
                send("POST","/employees",employee("未存在部署","missing@example.test",999),404);
                send("PUT","/employees/"+id,employee("変更失敗","alice@example.test",999),404);
            }
            case "prepare" -> send("POST","/employees",employee("D3社員B","bob@example.test",2),201);
            case "transfer" -> send("POST","/employees/transfers",batch(List.of(find("D3社員A"),find("D3社員B")),1),200);
            case "rollback" -> {
                send("POST","/employees/transfers",batch(List.of(find("D3社員A"),99999999L,find("D3社員B")),2),404);
                dbState("rollback-after");
            }
            case "errors" -> {
                send("POST","/employees",employee("入力不正","invalid-address",1),400);
                send("POST","/employees",employee("重複","alice@example.test",1),409);
                send("GET","/employees/99999999",null,404);
            }
            case "db-before","db-after" -> dbState(scenario);
            default -> throw new IllegalArgumentException("Unknown scenario: "+scenario);
        }
        System.out.println("PASS: "+scenario+" (actual status / response verified)");
    }
    private static String employee(String name,String email,int department) throws Exception {
        return JSON.writeValueAsString(Map.of("name",name,"email",email,"departmentId",department));
    }
    private static String batch(List<Long> ids,int department) throws Exception {
        return JSON.writeValueAsString(Map.of("employeeIds",ids,"departmentId",department));
    }
    private static long find(String name) throws Exception {
        var result=raw("GET","/employees?name="+encode(name),null);
        var list=JSON.readTree(result.body());
        if(result.statusCode()!=200 || list.size()!=1)throw new IllegalStateException("Scenario employee missing: "+name);
        return list.get(0).get("id").asLong();
    }
    private static void send(String method,String path,String body,int expected) throws Exception {
        var response=raw(method,path,body);
        System.out.println(method+" "+BASE+path);
        if(body!=null)System.out.println("Request: "+body);
        System.out.println("HTTP "+response.statusCode());
        response.headers().firstValue("location").ifPresent(v->System.out.println("Location: "+v));
        var value=JSON.readTree(response.body());
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        if(response.statusCode()!=expected)throw new IllegalStateException("Expected "+expected);
        if(expected>=400 && (value.size()!=3 || value.get("status").asInt()!=expected || !value.get("errors").isObject()))
            throw new IllegalStateException("Common error contract mismatch");
        System.out.println();
    }
    private static HttpResponse<String> raw(String method,String path,String body) throws Exception {
        var r=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(10));
        if(body!=null)r.header("Content-Type","application/json");
        return HTTP.send(r.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private static String encode(String value) {return URLEncoder.encode(value,StandardCharsets.UTF_8);}

    private static void dbState(String label) throws Exception {
        String log=Files.readString(Path.of("target/SPR-W6-D3-server.log"),StandardCharsets.UTF_8);
        var db=Pattern.compile("jdbc:h2:mem:sprw6d3-[a-zA-Z0-9-]+").matcher(log);
        if(!db.find())throw new IllegalStateException("Current Eclipse server JDBC URL missing in capture log");
        String jdbc=db.group();
        String landing=HTTP.send(HttpRequest.newBuilder(URI.create(BASE+"/h2-console/")).GET().build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        var session=Pattern.compile("jsessionid=([a-zA-Z0-9]+)").matcher(landing);
        if(!session.find())throw new IllegalStateException("H2 Console session missing");
        String sid=session.group(1);
        String login="driver=org.h2.Driver&url="+encode(jdbc)+"&user=sa&password=";
        form("/h2-console/login.do?jsessionid="+sid,login);
        String sql="SELECT E.ID,E.NAME,E.EMAIL,E.DEPARTMENT_ID,D.NAME AS DEPARTMENT_NAME FROM EMPLOYEE E JOIN DEPARTMENT D ON D.ID=E.DEPARTMENT_ID ORDER BY E.ID";
        String html=form("/h2-console/query.do?jsessionid="+sid,"sql="+encode(sql));
        Files.writeString(Path.of("target/SPR-W6-D3-"+label+"-h2.html"),html,StandardCharsets.UTF_8);
        var table=Pattern.compile("(?is)<table[^>]*class=\"resultSet\"[^>]*>(.*?)</table>").matcher(html);
        if(!table.find())throw new IllegalStateException("H2 SQL result table missing; inspect saved HTML");
        System.out.println("H2 Console independent JDBC connection: "+jdbc);
        System.out.println("SQL: SELECT E.ID,E.NAME,E.EMAIL,E.DEPARTMENT_ID,D.NAME AS DEPARTMENT_NAME");
        System.out.println("     FROM EMPLOYEE E JOIN DEPARTMENT D ON D.ID=E.DEPARTMENT_ID ORDER BY E.ID");
        System.out.println("Committed DB state / "+label+":");
        var rows=Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>").matcher(table.group(1));
        int count=0;
        while(rows.find()) {
            var cells=Pattern.compile("(?is)<t[dh][^>]*>(.*?)</t[dh]>").matcher(rows.group(1));
            List<String> values=new ArrayList<>();
            while(cells.find())values.add(text(cells.group(1)));
            System.out.println(String.join(" | ",values));count++;
        }
        if(count!=3)throw new IllegalStateException("Expected header + 2 committed employees, got "+count);
    }
    private static String form(String path,String body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(10))
            .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body));
        var response=HTTP.send(request.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode()!=200)throw new IllegalStateException("H2 Console HTTP "+response.statusCode());
        return response.body();
    }
    private static String text(String html) {
        String text=html.replaceAll("(?is)<[^>]+>","").replace("&nbsp;"," ").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&#39;","'").replace("&amp;","&");
        var matcher=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text);StringBuffer result=new StringBuffer();
        while(matcher.find()) {String v=matcher.group(1);int code=v.startsWith("x")?Integer.parseInt(v.substring(1),16):Integer.parseInt(v);matcher.appendReplacement(result,Matcher.quoteReplacement(new String(Character.toChars(code))));}
        matcher.appendTail(result);return result.toString().strip();
    }
}
