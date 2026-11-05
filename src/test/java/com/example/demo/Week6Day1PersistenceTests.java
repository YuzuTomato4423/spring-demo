package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.*;

/** SPR-W6-D1-01～04: 一時ファイルDBで再起動永続性と初期化を検証し、実dev DBには接続しない。 */
class Week6Day1PersistenceTests {
    @TempDir Path directory;
    private final ObjectMapper json=new ObjectMapper();
    @Test void fileRestartKeepsPostedIdAndExistingValuesWithoutDuplicateSamples()throws Exception {
        String url="jdbc:h2:file:"+directory.resolve("persistent").toString().replace('\\','/')+";DB_CLOSE_ON_EXIT=FALSE";
        JsonNode created;
        try(var app=start("dev","--spring.datasource.url="+url)) {
            var jdbc=app.getBean(JdbcTemplate.class);
            assertThat(app.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(2);
            created=send(app,"POST","/employees","{\"name\":\"W6PersistenceMarker\",\"departmentId\":1}",201);
            jdbc.update("UPDATE EMPLOYEE SET NAME='W6初期値を変更済' WHERE NAME='W6開発用_初期社員A'");
        }
        assertThat(Files.exists(directory.resolve("persistent.mv.db"))).isTrue();
        try(var app=start("dev","--spring.datasource.url="+url)) {
            assertThat(send(app,"GET","/employees/"+created.get("id").asLong(),null,200)).isEqualTo(created);
            var jdbc=app.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE WHERE NAME='W6初期値を変更済'",Integer.class)).isEqualTo(1);
        }
    }
    @Test void testUsesNewMemoryDatabaseAndNeverAddsDevEmployees()throws Exception {
        String firstUrl;
        try(var app=start("test")) {
            firstUrl=actualUrl(app);
            assertThat(firstUrl).startsWith("jdbc:h2:mem:sprw6d1-test-");
            assertThat(app.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");
            assertThat(app.getEnvironment().getProperty("spring.h2.console.enabled")).isEqualTo("false");
            assertThat(app.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isZero();
            send(app,"POST","/employees","{\"name\":\"TestOnlyMarker\",\"departmentId\":1}",201);
        }
        try(var app=start("test")) {
            assertThat(actualUrl(app)).isNotEqualTo(firstUrl);
            assertThat(send(app,"GET","/employees",null,200)).isEmpty();
        }
        try(var files=Files.list(directory)) { assertThat(files.findAny()).isEmpty(); }
    }
    private String actualUrl(ConfigurableApplicationContext app)throws Exception {
        try(var connection=app.getBean(javax.sql.DataSource.class).getConnection()) {
            return connection.getMetaData().getURL();
        }
    }
    private ConfigurableApplicationContext start(String profile,String...extra) {
        var args=new java.util.ArrayList<String>(java.util.List.of("--server.port=0"));
        args.addAll(java.util.List.of(extra));
        return new SpringApplicationBuilder(SpringDemoApplication.class).profiles("spr-w6-d1",profile).run(args.toArray(String[]::new));
    }
    private JsonNode send(ConfigurableApplicationContext app,String method,String path,String body,int status)throws Exception {
        String base="http://localhost:"+app.getEnvironment().getProperty("local.server.port");
        var request=HttpRequest.newBuilder(URI.create(base+path));
        if(body!=null)request.header("Content-Type","application/json");
        var response=HttpClient.newHttpClient().send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        return json.readTree(response.body());
    }
}
