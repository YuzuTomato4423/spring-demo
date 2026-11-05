package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.example.demo.controller.Week4Day1EmployeeController;
import com.example.demo.controller.Week4Day2EmployeeController;

/** 通常起動はD2であり、サンプル登録に依存せず空一覧を返せる。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.application.name=week4day2-default-test")
class Week4Day2DefaultHttpTests {
    @Autowired private Environment environment;
    @Autowired private ApplicationContext context;

    @Test void defaultProfileUsesDay2AndReturnsEmptyList() throws Exception {
        assertThat(context.getBeansOfType(Week4Day1EmployeeController.class)).isEmpty();
        assertThat(context.getBeansOfType(Week4Day2EmployeeController.class)).hasSize(1);
        URI uri = URI.create("http://localhost:" + environment.getRequiredProperty("local.server.port")
                + "/employees");
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }
}
