package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.example.demo.dto.*;
import com.example.demo.exception.*;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.service.EmployeeBusinessService;
import tools.jackson.databind.*;

/** SPR-W6-D3-01～04: HTTPのTransaction完了後に独立JDBCで確認。テスト全体はTransactionで囲まない。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("spr-w6-d3")
@ExtendWith(OutputCaptureExtension.class)
class Week6Day3BusinessTests {
    @Autowired Environment env;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmployeeBusinessService service;
    @Autowired EmployeeRegistrationNotifier notifier;
    @Autowired PlatformTransactionManager transactions;
    final HttpClient http=HttpClient.newHttpClient();
    final ObjectMapper json=new ObjectMapper();
    @BeforeEach void clearOnlyIsolatedTestEmployees() { jdbc.update("DELETE FROM EMPLOYEE"); }

    @Test void createNormalizesEmailAndCommitsBeforeNotification() throws Exception {
        int notifications=notifier.getDeliveryCount();
        var post=send("POST","/employees",body("正常登録","  Alice@Example.TEST  ",1));
        assertThat(post.statusCode()).as(post.body()).isEqualTo(201);
        var created=tree(post);long id=created.get("id").asLong();
        assertThat(created.size()).isEqualTo(4);
        assertThat(created.get("email").asText()).isEqualTo("alice@example.test");
        assertThat(post.headers().firstValue("location")).contains("/employees/"+id);
        assertThat(tree(send("GET","/employees/"+id,null))).isEqualTo(created);
        assertThat(jdbc.queryForObject("SELECT EMAIL FROM EMPLOYEE WHERE ID=?",String.class,id)).isEqualTo("alice@example.test");
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications+1);
    }
    @Test void duplicatePostReturns409AndDoesNotWriteOrNotify() throws Exception {
        create("重複元","duplicate@example.test",1);var before=rows();int count=notifier.getDeliveryCount();
        error(send("POST","/employees",body("重複"," DUPLICATE@EXAMPLE.TEST ",2)),409,"email");
        assertThat(rows()).isEqualTo(before);assertThat(notifier.getDeliveryCount()).isEqualTo(count);
    }
    @Test void updateKeepsOwnEmailButRejectsAnotherEmployeesEmail() throws Exception {
        long first=create("社員A","a@example.test",1),second=create("社員B","b@example.test",1);
        var result=send("PUT","/employees/"+first,body("社員A更新"," A@EXAMPLE.TEST ",2));
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(tree(result).get("email").asText()).isEqualTo("a@example.test");
        assertThat(jdbc.queryForObject("SELECT DEPARTMENT_ID FROM EMPLOYEE WHERE ID=?",Integer.class,first)).isEqualTo(2);
        var before=rows();
        error(send("PUT","/employees/"+second,body("拒否","a@example.test",2)),409,"email");
        assertThat(rows()).isEqualTo(before);
        assertThat(send("PUT","/employees/"+first,body("メール変更","new@example.test",1)).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT EMAIL FROM EMPLOYEE WHERE ID=?",String.class,first)).isEqualTo("new@example.test");
    }
    @Test void missingDepartmentOnCreateAndUpdateReturns404WithoutMutation() throws Exception {
        long id=create("部署確認","department@example.test",1);var before=rows();
        error(send("POST","/employees",body("未存在部署","missing@example.test",999)),404,"departmentId");
        error(send("PUT","/employees/"+id,body("変更失敗","department@example.test",999)),404,"departmentId");
        assertThat(rows()).isEqualTo(before);
    }
    @Test void invalidEmailsAndRequiredValuesReturn400WithoutSaving() throws Exception {
        for(String email:new String[]{null,"","   ","invalid-address","a".repeat(243)+"@example.test"}) {
            var values=new LinkedHashMap<String,Object>();values.put("name","入力確認");values.put("email",email);values.put("departmentId",1);
            error(send("POST","/employees",json.writeValueAsString(values)),400,"email");
        }
        error(send("POST","/employees",body(" ","valid@example.test",0)),400,"name");
        assertThat(rows()).isEmpty();
    }
    @Test void malformedJsonAndMissingEmployeesUseSameErrorContract() throws Exception {
        error(send("POST","/employees","{"),400,null);
        error(send("GET","/employees/not-a-number",null),400,null);
        for(String method:List.of("GET","PUT","DELETE"))
            error(send(method,"/employees/999999",method.equals("PUT")?body("社員","missing@example.test",1):null),404,null);
        assertThat(rows()).isEmpty();
    }
    @Test void transferCommitsEveryEmployeeAndPreservesNameEmailAndDepartments() throws Exception {
        long a=create("異動A","move-a@example.test",1),b=create("異動B","move-b@example.test",1);
        var before=rows();var departments=jdbc.queryForList("SELECT * FROM DEPARTMENT ORDER BY ID");
        var result=send("POST","/employees/transfers",transfer(List.of(a,b),2));
        assertThat(result.statusCode()).as(result.body()).isEqualTo(200);
        assertThat(tree(result).get("updatedCount").asInt()).isEqualTo(2);
        assertThat(tree(result).get("employees").size()).isEqualTo(2);
        var after=rows();
        for(int i=0;i<2;i++) {
            assertThat(after.get(i).get("DEPARTMENT_ID")).isEqualTo(2);
            assertThat(after.get(i).get("NAME")).isEqualTo(before.get(i).get("NAME"));
            assertThat(after.get(i).get("EMAIL")).isEqualTo(before.get(i).get("EMAIL"));
        }
        assertThat(jdbc.queryForList("SELECT * FROM DEPARTMENT ORDER BY ID")).isEqualTo(departments);
    }
    @Test void laterFailureRollsBackAlreadyFlushedFirstUpdate(CapturedOutput output) throws Exception {
        long a=create("戻るA","rollback-a@example.test",1),b=create("戻るB","rollback-b@example.test",1);
        var before=rows();
        error(send("POST","/employees/transfers",transfer(List.of(a,99999999L,b),2)),404,null);
        assertThat(output.getOut()).contains("[SPR-W6-D3-03] UPDATE flushed: employeeId="+a+", departmentId=2");
        assertThat(rows()).isEqualTo(before);
        assertThat(tree(send("GET","/employees/"+a,null)).get("departmentName").asText()).isEqualTo("開発部");
    }
    @Test void invalidBatchAndMissingDepartmentLeaveDatabaseUntouched() throws Exception {
        long a=create("入力保持","batch@example.test",1);var before=rows();
        for(String payload:List.of(transfer(List.of(),2),transfer(List.of(a,a),2),transfer(List.of(0L),2),
                transfer(List.of(-1L),2),"{\"employeeIds\":[null],\"departmentId\":2}","{\"departmentId\":2}",transfer(List.of(a),0)))
            error(send("POST","/employees/transfers",payload),400,null);
        error(send("POST","/employees/transfers",transfer(List.of(a),999)),404,"departmentId");
        assertThat(rows()).isEqualTo(before);
    }
    @Test void serviceRejectsDuplicateAndMissingDepartmentWithoutHttp() {
        service.create(new BusinessEmployeeRequest("直接A","direct@example.test",1));var before=rows();
        assertThatThrownBy(()->service.create(new BusinessEmployeeRequest("直接B"," DIRECT@EXAMPLE.TEST ",2)))
                .isInstanceOf(DuplicateEmployeeEmailException.class);
        assertThatThrownBy(()->service.create(new BusinessEmployeeRequest("直接C","other@example.test",999)))
                .isInstanceOf(DepartmentNotFoundException.class);
        assertThatThrownBy(()->service.create(new BusinessEmployeeRequest("直接D","",1)))
                .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThat(rows()).isEqualTo(before);
    }
    @Test void dbConstraintRejectsDirectDuplicateButAllowsLegacyNullEmails() throws Exception {
        create("DB元","db@example.test",1);
        assertThatThrownBy(()->jdbc.update("INSERT INTO EMPLOYEE(NAME,EMAIL,DEPARTMENT_ID) VALUES(?,?,?)","DB競合","db@example.test",2))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        jdbc.update("INSERT INTO EMPLOYEE(NAME,EMAIL,DEPARTMENT_ID) VALUES('旧課題A',NULL,1),('旧課題B',NULL,2)");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='EMPLOYEE' AND CONSTRAINT_NAME='UK_EMPLOYEE_EMAIL'")).hasSize(1);
    }
    @Test void simultaneousSameEmailRequestsCommitExactlyOnce() throws Exception {
        var start=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> request=()->{start.await();return send("POST","/employees",body("同時登録","race@example.test",1)).statusCode();};
            var first=pool.submit(request);var second=pool.submit(request);start.countDown();
            assertThat(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201,409);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE WHERE EMAIL='race@example.test'",Integer.class)).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    @Test void outerTransactionRollbackDoesNotSendRegistrationNotification() {
        int before=notifier.getDeliveryCount();
        assertThatThrownBy(()->new TransactionTemplate(transactions).executeWithoutResult(status->{
            service.create(new BusinessEmployeeRequest("通知しない","no-notify@example.test",1));
            throw new IllegalStateException("rollback verification");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(rows()).isEmpty();assertThat(notifier.getDeliveryCount()).isEqualTo(before);
    }
    @Test void existingSearchPagingSortingAndDeleteRemainAvailableInDay3() throws Exception {
        long a=create("互換A","compat-a@example.test",1);create("互換B","compat-b@example.test",2);
        var search=send("GET","/employees?name="+URLEncoder.encode("互換A",StandardCharsets.UTF_8),null);
        assertThat(tree(search).size()).isEqualTo(1);
        assertThat(tree(search).get(0).get("id").asLong()).isEqualTo(a);
        var page=send("GET","/employees?page=0&size=1&sort=name,desc",null);
        assertThat(page.statusCode()).as(page.body()).isEqualTo(200);
        assertThat(tree(page).get("totalElements").asInt()).isEqualTo(2);
        assertThat(send("DELETE","/employees/"+a,null).statusCode()).isEqualTo(204);
        error(send("GET","/employees/"+a,null),404,null);
        assertThat(env.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:sprw6d3-");
    }
    private long create(String name,String email,int department) throws Exception {
        var r=send("POST","/employees",body(name,email,department));assertThat(r.statusCode()).as(r.body()).isEqualTo(201);
        return tree(r).get("id").asLong();
    }
    private List<Map<String,Object>> rows() {return jdbc.queryForList("SELECT ID,NAME,EMAIL,DEPARTMENT_ID FROM EMPLOYEE ORDER BY ID");}
    private String body(String name,String email,int department) throws Exception {return json.writeValueAsString(Map.of("name",name,"email",email,"departmentId",department));}
    private String transfer(List<Long> ids,int department) throws Exception {return json.writeValueAsString(Map.of("employeeIds",ids,"departmentId",department));}
    private JsonNode tree(HttpResponse<String> response) throws Exception {return json.readTree(response.body());}
    private void error(HttpResponse<String> response,int status,String field) throws Exception {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);var value=tree(response);
        assertThat(value.size()).isEqualTo(3);assertThat(value.get("status").asInt()).isEqualTo(status);
        assertThat(value.get("message").asText()).isNotBlank();assertThat(value.get("errors").isObject()).isTrue();
        if(field!=null)assertThat(value.get("errors").has(field)).isTrue();
    }
    private HttpResponse<String> send(String method,String path,String payload) throws Exception {
        var r=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path)).timeout(Duration.ofSeconds(10));
        if(payload!=null)r.header("Content-Type","application/json");
        return http.send(r.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(payload,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
