package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.*;

/** SPR-W5-D5-01/02: 実HTTP完了後に独立JDBCでコミット状態を検証。テスト全体をTransactionで囲まない。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=jdbc:h2:mem:sprw5d5-test-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
    "spring.h2.console.enabled=false"
})
@ActiveProfiles({"spr-w5-d1","spr-w5-d2","spr-w5-d3","spr-w5-d4","spr-w5-d5"})
class Week5Day5SmokeTests {
    @Autowired Environment env;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client=HttpClient.newHttpClient();
    private final ObjectMapper json=new ObjectMapper();
    @BeforeEach void cleanIsolatedDatabase() { jdbc.update("DELETE FROM EMPLOYEE"); }

    @Test void integratedCrudSearchUsesSameCommittedJpaRecord() throws Exception {
        var post=send("POST","/employees",body("D5統合社員_作成",1),201);
        var created=json.readTree(post.body());
        long id=created.get("id").asLong();
        assertThat(id).isPositive();
        assertThat(post.headers().firstValue("location")).contains("/employees/"+id);
        assertThat(created.size()).isEqualTo(3);
        assertThat(read("/employees/"+id)).isEqualTo(created);
        assertThat(jdbc.queryForObject("SELECT NAME FROM EMPLOYEE WHERE ID=?",String.class,id)).isEqualTo("D5統合社員_作成");
        var updated=json.readTree(send("PUT","/employees/"+id,body("D5統合社員_更新",2),200).body());
        assertThat(updated.get("id").asLong()).isEqualTo(id);
        assertThat(updated.get("departmentName").asText()).isEqualTo("営業部");
        assertThat(read("/employees?name="+encode("統合社員")+"&department="+encode("営業部")).get(0)).isEqualTo(updated);
        assertThat(read("/employees?name="+encode("作成"))).isEmpty();
        assertThat(jdbc.queryForMap("SELECT NAME,DEPARTMENT_ID FROM EMPLOYEE WHERE ID=?",id))
            .containsEntry("NAME","D5統合社員_更新").containsEntry("DEPARTMENT_ID",2);
        assertThat(send("DELETE","/employees/"+id,null,204).body()).isEmpty();
        error(send("GET","/employees/"+id,null,404),404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE WHERE ID=?",Integer.class,id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DEPARTMENT",Integer.class)).isEqualTo(2);
    }

    @Test void validationAndNotFoundKeepCommonFormatAndDatabaseUnchanged() throws Exception {
        long id=json.readTree(send("POST","/employees",body("D5異常系_保持",1),201).body()).get("id").asLong();
        var employees=jdbc.queryForList("SELECT * FROM EMPLOYEE ORDER BY ID");
        var departments=jdbc.queryForList("SELECT * FROM DEPARTMENT ORDER BY ID");
        var invalid=error(send("POST","/employees",body("",0),400),400);
        assertThat(invalid.get("errors").has("name")).isTrue();
        assertThat(invalid.get("errors").has("departmentId")).isTrue();
        for(String payload:List.of(body("   ",0),body("部署不正",999)))
            error(send("PUT","/employees/"+id,payload,400),400);
        for(String method:List.of("GET","PUT","DELETE"))
            error(send(method,"/employees/"+(id+1000000),method.equals("PUT")?body("未存在",1):null,404),404);
        assertThat(jdbc.queryForList("SELECT * FROM EMPLOYEE ORDER BY ID")).isEqualTo(employees);
        assertThat(jdbc.queryForList("SELECT * FROM DEPARTMENT ORDER BY ID")).isEqualTo(departments);
    }
    private JsonNode read(String path) throws Exception { return json.readTree(send("GET",path,null,200).body()); }
    private JsonNode error(HttpResponse<String> response,int status) throws Exception {
        var value=json.readTree(response.body());
        assertThat(value.size()).isEqualTo(3);
        assertThat(value.get("status").asInt()).isEqualTo(status);
        assertThat(value.get("message").asText()).isNotBlank();
        assertThat(value.get("errors").isObject()).isTrue();
        return value;
    }
    private String body(String name,int departmentId) throws Exception { return json.writeValueAsString(Map.of("name",name,"departmentId",departmentId)); }
    private static String encode(String s) { return URLEncoder.encode(s,StandardCharsets.UTF_8); }
    private HttpResponse<String> send(String method,String path,String payload,int expected) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path)).timeout(Duration.ofSeconds(10));
        if(payload!=null)request.header("Content-Type","application/json");
        var response=client.send(request.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(payload,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as("%s %s: %s",method,path,response.body()).isEqualTo(expected);
        return response;
    }
}
