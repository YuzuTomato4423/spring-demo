package com.example.training.webmvc;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;
import tools.jackson.databind.*;

/** SPR-W6-D5-01: 実HTTPとH2 Consoleの独立JDBC照会を照合。観測値をそのままEclipse Consoleへ表示する。 */
public class Week6Day5HttpClient {
    private static final HttpClient HTTP=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String BASE="http://localhost:8080";
    private static final Path MARKER=Path.of("target/SPR-W6-D5-restart-marker.json");
    private static String jdbc;
    private static String session;

    public static void main(String[] args)throws Exception {
        String scenario=args.length==0?"api-search":args[0];
        System.out.println("=== SPR-W6-D5-01 / "+scenario+" / actual HTTP + independent JDBC ===");
        System.out.println(java.time.ZonedDateTime.now());
        connectDatabase(); // 社員を書き込む前に、現在のサーバーがD5専用DBへ接続していることを検証。
        switch(scenario) {
            case "prepare" -> {
                if(!dbState("empty-before-prepare",false).isEmpty())throw new IllegalStateException("D5 DB already contains employees. Back up and move the D5 database before repeating prepare.");
                var a=send("POST","/employees",employee("D5田中A"," D5-A@EXAMPLE.TEST ",1),201);
                var b=send("POST","/employees",employee("D5田中B","d5-b@example.test",2),201);
                var c=send("POST","/employees",employee("D5佐藤C","d5-c@example.test",1),201);
                a=send("PUT","/employees/"+a.get("id").asLong(),employee("D5田中A更新","d5-a@example.test",1),200);
                require(a.get("email").asText().equals("d5-a@example.test"),"Email normalization");
                Files.writeString(MARKER,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("employees",List.of(a,b,c))),StandardCharsets.UTF_8);
            }
            case "api-search" -> search();
            case "restart-before" -> {
                var marker=marker();
                verifyDetails(marker.get("employees"));
                var rows=dbState("restart-before",true);
                var data=new LinkedHashMap<String,Object>();data.put("employees",marker.get("employees"));data.put("rows",rows);data.put("jdbc",jdbc);
                Files.writeString(MARKER,JSON.writerWithDefaultPrettyPrinter().writeValueAsString(data),StandardCharsets.UTF_8);
                System.out.println("Saved restart baseline: same IDs / values / JDBC URL will be checked after restart.");
            }
            case "restart-after" -> {
                var marker=marker();
                require(marker.has("rows"),"Run restart-before first");
                require(jdbc.equals(marker.get("jdbc").asText()),"Same JDBC URL after restart");
                verifyDetails(marker.get("employees"));
                require(JSON.valueToTree(dbState("restart-after",true)).equals(marker.get("rows")),"Committed rows unchanged across restart");
                System.out.println("PASS: same employee IDs, updated values, department relations and file DB after server restart");
            }
            case "db-state" -> dbState("final-state",true);
            case "business-errors" -> errors();
            case "transaction" -> transaction();
            default -> throw new IllegalArgumentException("Unknown scenario: "+scenario);
        }
        System.out.println("PASS: "+scenario+" (actual HTTP status / response / committed DB verified)");
    }
    private static void search()throws Exception {
        var employees=marker().get("employees");
        long a=employees.get(0).get("id").asLong(),b=employees.get(1).get("id").asLong(),c=employees.get(2).get("id").asLong();
        query("name="+encode("田中"),List.of(a,b));
        query("department="+encode("開発部"),List.of(a,c));
        query("name="+encode("田中")+"&department="+encode("開発部"),List.of(a));
        query("name="+encode("佐藤")+"&department="+encode("営業部"),List.of());
        System.out.println("PASS: name partial match=2 / department exact match=2 / AND=1 / no match=0");
    }
    private static void query(String query,List<Long> expected)throws Exception {
        var result=send("GET","/employees?"+query+"&sort=id,asc",null,200);
        List<Long> ids=new ArrayList<>();for(var row:result){ids.add(row.get("id").asLong());require(row.has("departmentName"),"Public relation DTO");}
        require(ids.equals(expected),"Search expected "+expected+", actual "+ids);
    }
    private static void errors()throws Exception {
        var employees=marker().get("employees");long a=employees.get(0).get("id").asLong(),b=employees.get(1).get("id").asLong();
        var before=dbState("errors-before",false);
        send("POST","/employees",employee("重複拒否"," D5-A@EXAMPLE.TEST ",2),409);
        send("PUT","/employees/"+b,employee("重複更新拒否","d5-a@example.test",2),409);
        send("POST","/employees",employee("部署未存在","missing@example.test",999),404);
        send("PUT","/employees/"+a,employee("部署未存在","d5-a@example.test",999),404);
        send("POST","/employees",employee("入力不正","invalid-address",1),400);
        send("GET","/employees/99999999",null,404);
        require(dbState("errors-after",false).equals(before),"400/404/409 must not modify committed rows");
        System.out.println("PASS: all 6 expected errors use status/message/errors; committed DB unchanged");
    }
    private static void transaction()throws Exception {
        var employees=marker().get("employees");long a=employees.get(0).get("id").asLong(),b=employees.get(1).get("id").asLong();
        var result=send("POST","/employees/transfers",batch(List.of(a,b),2),200);
        require(result.get("updatedCount").asInt()==2,"Two committed transfers");
        var committed=dbState("transfer-committed",true);
        for(var row:committed)if(List.of(a,b).contains(Long.parseLong(row.get("ID"))))require(row.get("DEPARTMENT_ID").equals("2"),"Transfer committed");
        send("POST","/employees/transfers",batch(List.of(a,99999999L,b),1),404);
        require(dbState("transfer-rollback",true).equals(committed),"Flushed first update rolled back after missing employee");
        System.out.println("PASS: 2 employees committed to department 2; failed batch leaves all committed rows unchanged");
        String log=Files.readString(Path.of("target/SPR-W6-D5-server.log"),StandardCharsets.UTF_8);
        System.out.println("--- Actual server log excerpts (target/SPR-W6-D5-server.log) ---");
        var lines=log.lines().filter(x->x.contains("Committing JPA transaction")||x.contains("Rolling back JPA transaction")||x.contains("UPDATE flushed")||x.contains("[EMP-NOTIFY]")).toList();
        lines.subList(Math.max(0,lines.size()-14),lines.size()).forEach(System.out::println);
        require(log.contains("Rolling back JPA transaction"),"Actual rollback log");
    }
    private static void verifyDetails(JsonNode employees)throws Exception {
        for(var expected:employees)require(send("GET","/employees/"+expected.get("id").asLong(),null,200).equals(expected),"Persistent employee detail");
    }
    private static JsonNode marker()throws Exception{return JSON.readTree(Files.readString(MARKER,StandardCharsets.UTF_8));}
    private static String employee(String name,String email,int department)throws Exception{return JSON.writeValueAsString(Map.of("name",name,"email",email,"departmentId",department));}
    private static String batch(List<Long> ids,int department)throws Exception{return JSON.writeValueAsString(Map.of("employeeIds",ids,"departmentId",department));}
    private static JsonNode send(String method,String path,String body,int expected)throws Exception {
        var response=raw(method,path,body);
        var result=JSON.readTree(response.body());
        System.out.println(method+" "+URLDecoder.decode(path,StandardCharsets.UTF_8)+" -> HTTP "+response.statusCode());
        if(body!=null)System.out.println("Request: "+body);
        System.out.println("Response: "+JSON.writeValueAsString(result));
        require(response.statusCode()==expected,"Expected HTTP "+expected+" but got "+response.statusCode());
        if(expected==201)require(response.headers().firstValue("location").orElse("").equals("/employees/"+result.get("id").asLong()),"Location contract");
        if(expected>=400)require(result.size()==3&&result.get("status").asInt()==expected&&result.get("message").isString()&&result.get("errors").isObject(),"Common error contract");
        return result;
    }
    private static HttpResponse<String> raw(String method,String path,String body)throws Exception {
        var request=HttpRequest.newBuilder(URI.create(BASE+path)).timeout(Duration.ofSeconds(10));
        if(body!=null)request.header("Content-Type","application/json");
        return HTTP.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    private static void connectDatabase()throws Exception {
        String log=Files.readString(Path.of("target/SPR-W6-D5-server.log"),StandardCharsets.UTF_8);
        var url=Pattern.compile("jdbc:h2:file:[^\\s;,']+").matcher(log);
        require(url.find(),"D5 server JDBC URL missing");jdbc=url.group();
        String actual=jdbc.substring("jdbc:h2:file:".length()).replace('\\','/');
        require(Path.of(actual).toAbsolutePath().normalize().equals(Path.of("data/spr-w6-d5").toAbsolutePath().normalize()),"Server must use dedicated data/spr-w6-d5");
        String landing=raw("GET","/h2-console/",null).body();
        var sid=Pattern.compile("jsessionid=([a-zA-Z0-9]+)").matcher(landing);require(sid.find(),"H2 Console session");session=sid.group(1);
        form("login.do","driver=org.h2.Driver&url="+encode(jdbc)+"&user=sa&password=");
        System.out.println("Verified D5 JDBC URL before employee operations: "+jdbc);
    }
    private static List<Map<String,String>> dbState(String label,boolean display)throws Exception {
        String sql="SELECT E.ID,E.NAME,E.EMAIL,E.DEPARTMENT_ID,D.NAME AS DEPARTMENT_NAME FROM EMPLOYEE E JOIN DEPARTMENT D ON D.ID=E.DEPARTMENT_ID ORDER BY E.ID";
        String html=form("query.do","sql="+encode(sql));
        Files.writeString(Path.of("target/SPR-W6-D5-"+label+"-h2.html"),html,StandardCharsets.UTF_8);
        var table=Pattern.compile("(?is)<table[^>]*class=\"resultSet\"[^>]*>(.*?)</table>").matcher(html);require(table.find(),"H2 SELECT result");
        var trs=Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>").matcher(table.group(1));List<String> headers=null;List<Map<String,String>> result=new ArrayList<>();
        while(trs.find()){
            var cells=Pattern.compile("(?is)<t[dh][^>]*>(.*?)</t[dh]>").matcher(trs.group(1));List<String> values=new ArrayList<>();
            while(cells.find())values.add(text(cells.group(1)));
            if(headers==null){headers=values;require(headers.equals(List.of("ID","NAME","EMAIL","DEPARTMENT_ID","DEPARTMENT_NAME")),"DB headers");}
            else {require(values.size()==headers.size(),"DB column count");var row=new LinkedHashMap<String,String>();for(int i=0;i<headers.size();i++)row.put(headers.get(i),values.get(i));result.add(row);}
        }
        if(display){System.out.println("Independent JDBC SELECT / "+label+" / "+jdbc);System.out.println("ID | NAME | EMAIL | DEPARTMENT_ID | DEPARTMENT_NAME");for(var row:result)System.out.println(String.join(" | ",row.values()));}
        return result;
    }
    private static String form(String path,String body)throws Exception {
        var request=HttpRequest.newBuilder(URI.create(BASE+"/h2-console/"+path+"?jsessionid="+session)).header("Content-Type","application/x-www-form-urlencoded").timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.ofString(body));
        var response=HTTP.send(request.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));require(response.statusCode()==200,"H2 Console HTTP "+response.statusCode());return response.body();
    }
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private static String text(String html){
        String value=html.replaceAll("(?is)<[^>]+>","").replace("&nbsp;"," ").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&#39;","'").replace("&amp;","&");
        var matcher=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(value);StringBuffer output=new StringBuffer();
        while(matcher.find()){String s=matcher.group(1);int code=s.startsWith("x")?Integer.parseInt(s.substring(1),16):Integer.parseInt(s);matcher.appendReplacement(output,Matcher.quoteReplacement(new String(Character.toChars(code))));}matcher.appendTail(output);return output.toString().strip();
    }
}
