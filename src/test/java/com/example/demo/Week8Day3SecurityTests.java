package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.example.demo.service.EmployeeService;

// SPR-W8-D3-04: 実際の認証フィルターと社員 API を通し、ロールごとのアクセスと CSRF 保護を回帰確認する。
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("spr-w8-d3")
class Week8Day3SecurityTests {

    private static final String USERNAME = "training-user";
    private static final String ADMIN_NAME = "training-admin";
    private static final String USER_PASSWORD = UUID.randomUUID().toString();
    private static final String ADMIN_PASSWORD = UUID.randomUUID().toString();
    private static final String EMPLOYEE_BODY = """
            {"name":"Updated employee","departmentId":2}
            """;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EmployeeService employees;
    private int employeeId;

    @DynamicPropertySource
    static void trainingPasswords(DynamicPropertyRegistry properties) {
        properties.add("app.security.training.user-password", () -> USER_PASSWORD);
        properties.add("app.security.training.admin-password", () -> ADMIN_PASSWORD);
    }

    @BeforeEach
    void createEmployee() {
        employeeId = employees.create("Original employee", 1).getId();
    }

    @AfterEach
    void deleteEmployee() {
        employees.delete(employeeId);
    }

    @Test
    void helloIsPublic() throws Exception {
        mvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello Spring!"));
    }

    @Test
    void employeesRejectsIncorrectPassword() throws Exception {
        mvc.perform(get("/employees").header(HttpHeaders.AUTHORIZATION, basic(USERNAME, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void userAndAdminCanReadEmployees() throws Exception {
        assertCanReadEmployees(basic(USERNAME, USER_PASSWORD));
        assertCanReadEmployees(basic(ADMIN_NAME, ADMIN_PASSWORD));
    }

    @Test
    void sameUpdateReturns401ForAnonymous403ForUserAnd200ForAdmin() throws Exception {
        var anonymous = mvc.perform(withCsrf(updateRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andReturn();
        var user = mvc.perform(withCsrf(updateRequest()).header(HttpHeaders.AUTHORIZATION, basic(USERNAME, USER_PASSWORD)))
                .andExpect(status().isForbidden())
                .andReturn();
        assertThat(employees.findById(employeeId).getName()).isEqualTo("Original employee");

        var admin = mvc.perform(withCsrf(updateRequest()).header(HttpHeaders.AUTHORIZATION, basic(ADMIN_NAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employeeId))
                .andExpect(jsonPath("$.name").value("Updated employee"))
                .andReturn();
        assertThat(employees.findById(employeeId).getName()).isEqualTo("Updated employee");

        System.out.printf("PUT /employees/%d (same JSON body, valid CSRF token)%n", employeeId);
        System.out.printf("  ADMIN       -> HTTP %d  (update permitted)%n", admin.getResponse().getStatus());
        System.out.printf("  USER        -> HTTP %d  (authenticated, role denied)%n", user.getResponse().getStatus());
        System.out.printf("  ANONYMOUS   -> HTTP %d  (no credentials)%n", anonymous.getResponse().getStatus());
    }

    @Test
    void adminCannotUpdateWithoutCsrfToken() throws Exception {
        mvc.perform(updateRequest().header(HttpHeaders.AUTHORIZATION, basic(ADMIN_NAME, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden());
        assertThat(employees.findById(employeeId).getName()).isEqualTo("Original employee");
    }

    @Test
    void onlyAdminCanCreateAndDeleteEmployees() throws Exception {
        String user = basic(USERNAME, USER_PASSWORD);
        String admin = basic(ADMIN_NAME, ADMIN_PASSWORD);

        mvc.perform(withCsrf(post("/employees").contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE_BODY))
                .header(HttpHeaders.AUTHORIZATION, user)).andExpect(status().isForbidden());
        String location = mvc.perform(withCsrf(post("/employees").contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE_BODY))
                .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mvc.perform(withCsrf(delete(location)).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
        mvc.perform(withCsrf(delete(location)).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mvc.perform(get(location).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNotFound());
    }

    @Test
    void adminCannotAccessUnlistedPaths() throws Exception {
        mvc.perform(get("/unlisted").header(HttpHeaders.AUTHORIZATION, basic(ADMIN_NAME, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private void assertCanReadEmployees(String authorization) throws Exception {
        mvc.perform(get("/employees").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(employeeId));
        mvc.perform(get("/employees/{id}", employeeId).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original employee"));
    }

    private MockHttpServletRequestBuilder updateRequest() {
        return put("/employees/{id}", employeeId).contentType(MediaType.APPLICATION_JSON).content(EMPLOYEE_BODY);
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder mutation) throws Exception {
        // 公開 GET のリクエスト属性から実際のフィルターが発行したトークンを取得し、同じセッションで返す。
        var request = mvc.perform(get("/hello")).andReturn().getRequest();
        var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        String value = token.getToken();
        return mutation.session((MockHttpSession) request.getSession(false)).header(token.getHeaderName(), value);
    }

    private static String basic(String username, String password) {
        byte[] credentials = (username + ":" + password).getBytes(StandardCharsets.UTF_8);
        return "Basic " + Base64.getEncoder().encodeToString(credentials);
    }
}
