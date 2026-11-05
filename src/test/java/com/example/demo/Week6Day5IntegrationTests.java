package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import tools.jackson.databind.*;

/**
 * SPR-W6-D5-01/SPR-W6-D5-02/SPR-W6-D5-03: 実HTTP→Service→Repository→一時file DBを通し、独立JDBCで結果を検証する。
 * 実習用data配下へ接続せず、テスト全体をTransactionで囲まない。再起動ではContextも作り直す。
 */
@DisplayName("SPR-W6-D5-01 永続DB・API統合")
class Week6Day5IntegrationTests {
    @TempDir Path directory;
    final ObjectMapper json=new ObjectMapper();
    final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Test @DisplayName("再起動後もID・更新値・関連検索・部署件数を保持")
    void restartKeepsBusinessEmployeesAndRelatedSearch() throws Exception {
        JsonNode first,second,third; List<Map<String,Object>> before;
        try(var app=start()) {
            assertThat(app.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
            first=send(app,"POST","/employees",employee("D5田中A"," A@EXAMPLE.TEST ",1),201);
            second=send(app,"POST","/employees",employee("D5田中B","b@example.test",2),201);
            third=send(app,"POST","/employees",employee("D5佐藤C","c@example.test",1),201);
            first=send(app,"PUT","/employees/"+first.get("id").asLong(),employee("D5田中A更新","a@example.test",1),200);
            assertThat(first.get("email").asText()).isEqualTo("a@example.test");
            assertThat(app.getBean(EmployeeRegistrationNotifier.class).getDeliveryCount()).isEqualTo(3);
            search(app,"田中",null,List.of(first,second));
            search(app,null,"開発部",List.of(first,third));
            search(app,"田中","開発部",List.of(first));
            search(app,"佐藤","営業部",List.of());
            before=rows(app);
        }
        assertThat(Files.exists(directory.resolve("integration.mv.db"))).isTrue();
        try(var app=start()) {
            assertThat(rows(app)).isEqualTo(before);
            for(var expected:List.of(first,second,third))
                assertThat(send(app,"GET","/employees/"+expected.get("id").asLong(),null,200)).isEqualTo(expected);
            search(app,"田中","開発部",List.of(first));
            assertThat(jdbc(app).queryForObject("SELECT COUNT(*) FROM DEPARTMENT",Integer.class)).isEqualTo(2);
            assertThat(app.getBean(EmployeeRegistrationNotifier.class).getDeliveryCount()).isZero();
        }
    }
    @Test @DisplayName("400/404/409は共通形式・DBと通知不変")
    void businessErrorsDoNotMutateCommittedState() throws Exception {
        try(var app=start()) {
            var a=send(app,"POST","/employees",employee("社員A","a@example.test",1),201);
            var b=send(app,"POST","/employees",employee("社員B","b@example.test",2),201);
            var before=rows(app);
            error(app,"POST","/employees",employee("重複"," A@EXAMPLE.TEST ",1),409,"email");
            error(app,"PUT","/employees/"+b.get("id").asLong(),employee("重複更新","a@example.test",1),409,"email");
            error(app,"POST","/employees",employee("部署未存在","missing@example.test",999),404,"departmentId");
            error(app,"PUT","/employees/"+a.get("id").asLong(),employee("部署未存在","a@example.test",999),404,"departmentId");
            error(app,"POST","/employees",employee("入力不正","invalid",1),400,"email");
            error(app,"GET","/employees/99999999",null,404,null);
            assertThat(rows(app)).isEqualTo(before);
            assertThat(app.getBean(EmployeeRegistrationNotifier.class).getDeliveryCount()).isEqualTo(2);
        }
    }
    @Test @DisplayName("一括異動は全件commit・後続失敗時はflush済み更新もrollback")
    void transferCommitsAndRollsBackAsOneUnit() throws Exception {
        try(var app=start()) {
            var a=send(app,"POST","/employees",employee("異動A","move-a@example.test",1),201);
            var b=send(app,"POST","/employees",employee("異動B","move-b@example.test",1),201);
            var ids=List.of(a.get("id").asLong(),b.get("id").asLong());
            var result=send(app,"POST","/employees/transfers",batch(ids,2),200);
            assertThat(result.get("updatedCount").asInt()).isEqualTo(2);
            assertThat(jdbc(app).queryForList("SELECT DEPARTMENT_ID FROM EMPLOYEE ORDER BY ID",Integer.class)).containsExactly(2,2);
            var before=rows(app);
            error(app,"POST","/employees/transfers",batch(List.of(ids.get(0),99999999L,ids.get(1)),1),404,null);
            assertThat(rows(app)).isEqualTo(before);
            assertThat(app.getBean(EmployeeRegistrationNotifier.class).getDeliveryCount()).isEqualTo(2);
        }
    }
    private ConfigurableApplicationContext start() throws Exception {
        String file=directory.resolve("integration").toString().replace('\\','/');
        var app=new SpringApplicationBuilder(SpringDemoApplication.class).profiles("spr-w6-d5")
            .run("--server.port=0","--spring.datasource.url=jdbc:h2:file:"+file+";DB_CLOSE_ON_EXIT=FALSE","--spring.h2.console.enabled=false");
        try(var connection=jdbc(app).getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:h2:file:"+file);
        } catch(Throwable ex) {app.close();throw ex;}
        return app;
    }
    private JdbcTemplate jdbc(ConfigurableApplicationContext app){return app.getBean(JdbcTemplate.class);}
    private List<Map<String,Object>> rows(ConfigurableApplicationContext app){return jdbc(app).queryForList("SELECT ID,NAME,EMAIL,DEPARTMENT_ID FROM EMPLOYEE ORDER BY ID");}
    private String employee(String name,String email,int department)throws Exception{return json.writeValueAsString(Map.of("name",name,"email",email,"departmentId",department));}
    private String batch(List<Long> ids,int department)throws Exception{return json.writeValueAsString(Map.of("employeeIds",ids,"departmentId",department));}
    private JsonNode send(ConfigurableApplicationContext app,String method,String path,String body,int expected)throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://localhost:"+app.getEnvironment().getProperty("local.server.port")+path)).timeout(Duration.ofSeconds(10));
        if(body!=null)req.header("Content-Type","application/json");
        var response=http.send(req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);
        var result=json.readTree(response.body());
        if(expected==201)assertThat(response.headers().firstValue("location")).contains("/employees/"+result.get("id").asLong());
        return result;
    }
    private void error(ConfigurableApplicationContext app,String method,String path,String body,int status,String field)throws Exception {
        var r=send(app,method,path,body,status);
        assertThat(r.size()).isEqualTo(3);assertThat(r.get("status").asInt()).isEqualTo(status);
        assertThat(r.get("message").asText()).isNotBlank();assertThat(r.get("errors").isObject()).isTrue();
        if(field!=null)assertThat(r.get("errors").has(field)).isTrue();
    }
    private void search(ConfigurableApplicationContext app,String name,String department,List<JsonNode> expected)throws Exception {
        String query="/employees?sort=id,asc";
        if(name!=null)query+="&name="+URLEncoder.encode(name,java.nio.charset.StandardCharsets.UTF_8);
        if(department!=null)query+="&department="+URLEncoder.encode(department,java.nio.charset.StandardCharsets.UTF_8);
        var found=send(app,"GET",query,null,200);List<Long> ids=new ArrayList<>();
        for(var row:found){ids.add(row.get("id").asLong());assertThat(row.get("departmentName").asText()).isNotBlank();}
        assertThat(ids).containsExactlyElementsOf(expected.stream().map(x->x.get("id").asLong()).toList());
    }
}
