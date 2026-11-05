package com.example.demo;

import static org.assertj.core.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/** SPR-W5-D5-03: 同じ名前のメモリDBで実アプリを閉じて再起動し、create-dropと初期データを検証する。 */
class Week5Day5RestartTests {
    @Test void restartResetsEmployeesAndRecreatesTwoDepartments() throws Exception {
        String url="jdbc:h2:mem:sprw5d5-restart-"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        try(var first=start(url)) {
            String base="http://localhost:"+first.getEnvironment().getProperty("local.server.port");
            var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/employees"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"RestartMarker\",\"departmentId\":1}")).build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(first.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isEqualTo(1);
        }
        try(var second=start(url)) {
            var jdbc=second.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE",Integer.class)).isZero();
            assertThat(jdbc.queryForList("SELECT NAME FROM DEPARTMENT ORDER BY ID",String.class)).containsExactly("開発部","営業部");
            String base="http://localhost:"+second.getEnvironment().getProperty("local.server.port");
            var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/employees")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("[]");
        }
    }
    private ConfigurableApplicationContext start(String url) {
        return new SpringApplicationBuilder(SpringDemoApplication.class)
            .profiles("spr-w5-d1","spr-w5-d2","spr-w5-d3","spr-w5-d4","spr-w5-d5")
            .run("--server.port=0","--spring.datasource.url="+url,"--spring.h2.console.enabled=false");
    }
}
