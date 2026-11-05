package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.*;

/** SPR-W5-D4: 実HTTP・独立JDBCと発行SQLで確認し、テスト全体をTransactionで囲まない。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:sprw5d4-test-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
    "spring.h2.console.enabled=false",
    "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.example.demo.Week5Day4HttpTests$SqlCapture"
})
@ActiveProfiles({"spr-w5-d1","spr-w5-d2","spr-w5-d3","spr-w5-d4","spr-w5-d4-rollback"})
public class Week5Day4HttpTests {
    @Autowired Environment env;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client=HttpClient.newHttpClient();
    private final ObjectMapper json=new ObjectMapper();

    public static class SqlCapture implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final List<String> sql=new CopyOnWriteArrayList<>();
        @Override public String inspect(String value) { sql.add(value.toLowerCase(Locale.ROOT)); return value; }
    }
    @BeforeEach void setup() throws Exception {
        jdbc.update("DELETE FROM EMPLOYEE");
        jdbc.execute("ALTER TABLE EMPLOYEE ALTER COLUMN ID RESTART WITH 1");
        post("Charlie",1); post("Alpha",1); post("Bravo",2); post("Alpha",2);
        SqlCapture.sql.clear();
    }
    @Test void putCommitsNameAndDepartmentWithSameId() throws Exception {
        var updated=send("PUT","/employees/1",body("Delta",2),200);
        assertThat(updated.get("id").asLong()).isEqualTo(1L);
        assertThat(updated.get("departmentName").asText()).isEqualTo("営業部");
        assertThat(jdbc.queryForMap("SELECT NAME,DEPARTMENT_ID FROM EMPLOYEE WHERE ID=1"))
                .containsEntry("NAME","Delta").containsEntry("DEPARTMENT_ID",2);
        assertThat(send("GET","/employees/1",null,200)).isEqualTo(updated);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(4);
    }
    @Test void runtimeExceptionAfterRealSqlRollsBackCommittedDatabase() throws Exception {
        var before=jdbc.queryForMap("SELECT * FROM EMPLOYEE WHERE ID=1");
        var dto=send("GET","/employees/1",null,200);
        SqlCapture.sql.clear();
        var failure=send("POST","/training/rollback/employees/1",body("TEMP",2),500);
        assertThat(failure.get("message").asText()).contains("検証用例外");
        assertThat(SqlCapture.sql).anyMatch(sql->sql.startsWith("update employee"));
        assertThat(jdbc.queryForMap("SELECT * FROM EMPLOYEE WHERE ID=1")).isEqualTo(before);
        assertThat(send("GET","/employees/1",null,200)).isEqualTo(dto);
    }
    @Test void pagesHaveCorrectTotalsNoOverlapAndDatabaseLimit() throws Exception {
        var zero=get("?page=0&size=2");
        var one=get("?page=1&size=2");
        assertThat(ids(zero.get("content"))).containsExactly(1L,2L);
        assertThat(ids(one.get("content"))).containsExactly(3L,4L);
        assertThat(zero.get("number").asInt()).isZero();
        assertThat(one.get("number").asInt()).isEqualTo(1);
        assertThat(zero.get("size").asInt()).isEqualTo(2);
        assertThat(zero.get("totalElements").asLong()).isEqualTo(4);
        assertThat(zero.get("totalPages").asInt()).isEqualTo(2);
        assertThat(zero.get("first").asBoolean()).isTrue();
        assertThat(zero.get("last").asBoolean()).isFalse();
        assertThat(one.get("last").asBoolean()).isTrue();
        assertThat(zero.size()).isEqualTo(7);
        assertThat(zero.get("content").get(0).size()).isEqualTo(3);
        assertThat(SqlCapture.sql).anyMatch(sql->sql.contains("offset") && sql.contains("fetch first") && sql.contains("order by"));
    }
    @Test void omittedPagingPreservesArrayAndSingleParameterUsesDefaults() throws Exception {
        assertThat(get("").isArray()).isTrue();
        assertThat(ids(get(""))).containsExactly(1L,2L,3L,4L);
        assertThat(get("?sort=name,asc").isArray()).isTrue();
        assertThat(get("?page=0").get("size").asInt()).isEqualTo(20);
        assertThat(get("?size=1").get("number").asInt()).isZero();
    }
    @Test void allSearchCombinationsSharePageAndCountConditions() throws Exception {
        for(String query:List.of("?name=Alpha","?department="+encode("営業部"))) {
            var value=get(query+"&page=0&size=1");
            assertThat(value.get("content").size()).isEqualTo(1);
            assertThat(value.get("totalElements").asLong()).isEqualTo(2);
            assertThat(value.get("totalPages").asInt()).isEqualTo(2);
        }
        var combined=get("?name="+encode(" Alpha ")+"&department="+encode(" 営業部 ")+"&page=0&size=1");
        assertThat(ids(combined.get("content"))).containsExactly(4L);
        assertThat(combined.get("totalElements").asLong()).isEqualTo(1);
        assertThat(get("?name=%20&department=%20&page=0&size=2").get("totalElements").asLong()).isEqualTo(4);
        post("100%_literal",1);
        assertThat(get("?name="+encode("%_")+"&page=0&size=2").get("totalElements").asLong()).isEqualTo(1);
    }
    @Test void emptyAndOutOfRangePagesAreSuccessful() throws Exception {
        assertThat(get("?page=99&size=2").get("content").isEmpty()).isTrue();
        assertThat(get("?page=99&size=2").get("totalElements").asLong()).isEqualTo(4);
        var empty=get("?name=missing&page=0&size=2");
        assertThat(empty.get("content").isEmpty()).isTrue();
        assertThat(empty.get("totalPages").asInt()).isZero();
    }
    @Test void sortDirectionsMultiSortAndTiesWorkOnArraysAndPages() throws Exception {
        assertThat(ids(get("?sort=id,asc"))).containsExactly(1L,2L,3L,4L);
        assertThat(ids(get("?sort=id,desc"))).containsExactly(4L,3L,2L,1L);
        assertThat(ids(get("?sort=name,asc"))).containsExactly(2L,4L,3L,1L);
        assertThat(ids(get("?sort=name,desc"))).containsExactly(1L,3L,2L,4L);
        assertThat(ids(get("?sort=name,asc&sort=id,desc"))).containsExactly(4L,2L,3L,1L);
        assertThat(ids(get("?sort=name,asc&page=0&size=1").get("content"))).containsExactly(2L);
        assertThat(ids(get("?sort=name,asc&page=1&size=1").get("content"))).containsExactly(4L);
    }
    @Test void invalidListOptionsKeepCommon400AndDoNotWrite() throws Exception {
        for(String query:List.of("page=-1","size=0","size=-1","page=abc","size=abc","page=",
                "size=","page=2147483648","page=2147483647&size=2","page=0&page=1",
                "sort=name","sort=name,sideways","sort=department,asc","sort=","sort=id,asc,desc",
                "sort=name,asc&sort=name,desc")) {
            var error=send("GET","/employees?"+query,null,400);
            assertThat(error.get("status").asInt()).isEqualTo(400);
            assertThat(error.get("message").asText()).isNotBlank();
            assertThat(error.get("errors").isObject()).isTrue();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(4);
    }
    private JsonNode post(String name,int department) throws Exception { return send("POST","/employees",body(name,department),201); }
    private JsonNode get(String query) throws Exception { return send("GET","/employees"+query,null,200); }
    private String body(String name,int department) throws Exception { return json.writeValueAsString(Map.of("name",name,"departmentId",department)); }
    private static String encode(String text) { return URLEncoder.encode(text,StandardCharsets.UTF_8); }
    private static List<Long> ids(JsonNode items) { List<Long> ids=new ArrayList<>(); for(var item:items) ids.add(item.get("id").asLong()); return ids; }
    private JsonNode send(String method,String path,String body,int expected) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+env.getProperty("local.server.port")+path))
                .timeout(java.time.Duration.ofSeconds(10));
        if(body!=null) request.header("Content-Type","application/json");
        var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():
                HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)).build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as("%s %s: %s",method,path,response.body()).isEqualTo(expected);
        return json.readTree(response.body());
    }
}
