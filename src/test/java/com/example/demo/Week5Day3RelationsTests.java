package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.demo.repository.DepartmentJpaRepository;
import com.example.demo.repository.EmployeeJpaRepository;
import tools.jackson.databind.*;

/** SPR-W5-D3: 実HTTP、別JDBC取得、JPA再取得を使って関連と検索を検証する。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=jdbc:h2:mem:sprw5d3-test-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
    "spring.h2.console.enabled=false", "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles({"spr-w5-d1","spr-w5-d2","spr-w5-d3"})
class Week5Day3RelationsTests {
    @Autowired Environment env;
    @Autowired JdbcTemplate jdbc;
    @Autowired DepartmentJpaRepository departments;
    @Autowired EmployeeJpaRepository employees;
    @Autowired PlatformTransactionManager transactions;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    private final HttpClient client=HttpClient.newHttpClient();
    private final ObjectMapper json=new ObjectMapper();

    @BeforeEach void setup() throws Exception {
        jdbc.update("DELETE FROM EMPLOYEE");
        post("田中太郎",1); post("田中太郎",2); post("佐藤花子",1); post("100%_確認",1);
    }
    @Test void persistedDepartmentRelationAndForeignKeyAreReal() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            em.clear();
            assertThat(departments.findById(1).orElseThrow().getName()).isEqualTo("開発部");
            var employee=employees.findByNameContainingAndDepartment_Name("田中","開発部").get(0);
            assertThat(employee.getDepartment().getId()).isEqualTo(1);
            assertThat(employee.getDepartment().getName()).isEqualTo("開発部");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS "
                +"WHERE CONSTRAINT_NAME='FK_EMPLOYEE_DEPARTMENT'",Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO EMPLOYEE(NAME,DEPARTMENT_ID) VALUES('不正部署',999)"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(4);
    }
    @Test void searchUsesNameAndRelatedDepartmentWithExistingWhitespaceRules() throws Exception {
        assertThat(get("?name="+encode("田中")).size()).isEqualTo(2);
        assertThat(get("?department="+encode("開発部")).size()).isEqualTo(3);
        var combined=get("?name="+encode(" 田中 ")+"&department="+encode(" 開発部 "));
        assertThat(combined.size()).isEqualTo(1);
        assertThat(combined.get(0).get("departmentName").asText()).isEqualTo("開発部");
        assertThat(get("?name="+encode("田中")+"&department="+encode("開発")).isEmpty()).isTrue();
        assertThat(get("?name=missing").isEmpty()).isTrue();
        assertThat(get("?name=%20&department=%20").size()).isEqualTo(4);
        assertThat(get("?name="+encode("%_")).size()).isEqualTo(1);
    }
    @Test void jsonExposesOnlyDtoAndLongIdsWithoutOpenSessionInView() throws Exception {
        jdbc.execute("ALTER TABLE EMPLOYEE ALTER COLUMN ID RESTART WITH 2147483650");
        var created=post("Long関連確認",2);
        assertThat(created.get("id").asLong()).isGreaterThan(Integer.MAX_VALUE);
        assertThat(created.size()).isEqualTo(3);
        assertThat(created.has("department")).isFalse();
        assertThat(created.has("departmentId")).isFalse();
        var response=send("GET","/employees/"+created.get("id").asLong(),null,200);
        assertThat(json.readTree(response.body())).isEqualTo(created);
        assertThat(env.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }
    @Test void employeeDeletionKeepsSharedDepartment() throws Exception {
        long id=get("?name="+encode("佐藤")).get(0).get("id").asLong();
        send("DELETE","/employees/"+id,null,204);
        assertThat(departments.count()).isEqualTo(2);
        assertThat(get("?department="+encode("開発部")).size()).isEqualTo(2);
    }
    @Test void dtoListLoadsAllDepartmentsInOneSelect() throws Exception {
        var stats=em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        assertThat(get("").size()).isEqualTo(4);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }
    private JsonNode post(String name,int department) throws Exception {
        return json.readTree(send("POST","/employees",json.writeValueAsString(
                java.util.Map.of("name",name,"departmentId",department)),201).body());
    }
    private JsonNode get(String query) throws Exception { return json.readTree(send("GET","/employees"+query,null,200).body()); }
    private static String encode(String value) { return URLEncoder.encode(value,StandardCharsets.UTF_8); }
    private HttpResponse<String> send(String method,String path,String body,int expected) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path))
                .timeout(java.time.Duration.ofSeconds(10));
        if(body!=null) request.header("Content-Type","application/json");
        var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():
                HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);
        return response;
    }
}
