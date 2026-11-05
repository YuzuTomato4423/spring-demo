package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.example.demo.entity.DepartmentEntity;
import tools.jackson.databind.*;

/** SPR-W6-D2: 実HTTP・JDBC・再取得した関連で確認。リクエストごとに新規永続化コンテキストを使う。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("spr-w6-d2")
abstract class AbstractWeek6Day2RelationsTests {
    @Autowired Environment env;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @PersistenceContext EntityManager em;
    final HttpClient client=HttpClient.newHttpClient();
    final ObjectMapper mapper=new ObjectMapper();
    abstract boolean eager();
    HttpResponse<String> send(String method,String path,String body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path))
            .timeout(java.time.Duration.ofSeconds(10));
        if(body!=null)request.header("Content-Type","application/json");
        return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():
            HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
    int selects(HttpResponse<String> response,String header) {return Integer.parseInt(response.headers().firstValue(header).orElseThrow());}

    @Test void plainShowsFetchTimingAndNPlusOneOnEveryFreshRequest() throws Exception {
        for(int i=0;i<2;i++) {
            var response=send("GET","/training/departments?fetch=plain",null);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            assertThat(selects(response,"X-Training-After-Load-Selects")).isEqualTo(eager()?4:1);
            assertThat(selects(response,"X-Training-Total-Selects")).isEqualTo(4);
            assertThat(response.headers().firstValue("X-Training-Initially-Loaded")).contains(
                eager()?"[true, true, true]":"[false, false, false]");
        }
    }
    @Test void joinUsesOneSelectAndReturnsIdenticalDtosIncludingEmptyDepartment() throws Exception {
        var plain=send("GET","/training/departments?fetch=plain",null);
        var joined=send("GET","/training/departments?fetch=join",null);
        assertThat(joined.statusCode()).isEqualTo(200);
        assertThat(selects(joined,"X-Training-After-Load-Selects")).isEqualTo(1);
        assertThat(selects(joined,"X-Training-Total-Selects")).isEqualTo(1);
        assertThat(joined.headers().firstValue("X-Training-Initially-Loaded")).contains("[true, true, true]");
        var json=mapper.readTree(joined.body());
        assertThat(json).isEqualTo(mapper.readTree(plain.body()));
        assertThat(json.size()).isEqualTo(3);
        for(int i=0;i<3;i++) {
            assertThat(json.get(i).get("id").asInt()).isEqualTo(i+1);
            assertThat(json.get(i).size()).isEqualTo(3);
            assertThat(json.get(i).get("employees").size()).isEqualTo(i==2?0:2);
            for(var employee:json.get(i).get("employees")) {
                assertThat(employee.size()).isEqualTo(2);
                assertThat(employee.has("department")).isFalse();
                assertThat(employee.get("id").asLong()).isPositive();
                assertThat(employee.get("name").asText()).isNotBlank();
            }
        }
        assertThat(env.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
        assertThat(env.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:sprw6d2-");
    }
    @Test void foreignKeyAndBothDirectionsReferToSamePersistedRows() {
        new TransactionTemplate(transactions).executeWithoutResult(status->{
            em.clear();
            DepartmentEntity department=em.find(DepartmentEntity.class,1);
            assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(department,"employees")).isEqualTo(eager());
            assertThat(department.getEmployees()).hasSize(2).allSatisfy(employee->{
                assertThat(employee.getDepartment()).isSameAs(department);
                assertThat(jdbc.queryForObject("SELECT DEPARTMENT_ID FROM EMPLOYEE WHERE ID=?",Integer.class,employee.getId())).isEqualTo(1);
            });
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME='FK_EMPLOYEE_DEPARTMENT'",Integer.class)).isEqualTo(1);
        assertThatThrownBy(()->jdbc.update("INSERT INTO EMPLOYEE(NAME,DEPARTMENT_ID) VALUES('invalid',999)"))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(4);
    }
    @Test void defaultFetchAndInvalidFetchHaveDefinedResponses() throws Exception {
        var response=send("GET","/training/departments",null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(selects(response,"X-Training-Total-Selects")).isEqualTo(4);
        assertThat(send("GET","/training/departments?fetch=unknown",null).statusCode()).isEqualTo(400);
    }
    @Test void existingEmployeeApiStillSupportsCrudAndKeepsDepartments() throws Exception {
        var created=send("POST","/employees","{\"name\":\"W6 CRUD\",\"departmentId\":1}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        long id=mapper.readTree(created.body()).get("id").asLong();
        try {
            var update=send("PUT","/employees/"+id,"{\"name\":\"W6 updated\",\"departmentId\":2}");
            assertThat(update.statusCode()).as(update.body()).isEqualTo(200);
            var got=send("GET","/employees/"+id,null);
            assertThat(got.statusCode()).isEqualTo(200);
            var dto=mapper.readTree(got.body());
            assertThat(dto.size()).isEqualTo(3);
            assertThat(dto.get("departmentName").asText()).isEqualTo("営業部");
        } finally { assertThat(send("DELETE","/employees/"+id,null).statusCode()).isEqualTo(204); }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DEPARTMENT",Integer.class)).isEqualTo(3);
    }
}
