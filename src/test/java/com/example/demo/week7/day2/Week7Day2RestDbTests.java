package com.example.demo.week7.day2;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SPR-W7-D2-04: 同じIDの登録→取得→更新→削除を実HTTPで通し、応答だけでなくcommit後の
 * DB内容もJDBCで確認する。モックでは見逃す配線・永続化・Transactionの回帰を検出する。
 */
@DisplayName("SPR-W7-D2-04 REST to DB CRUD")
class Week7Day2RestDbTests extends Week7Day2IntegrationSupport {
    @Test
    @DisplayName("SPR-W7-D2-04 POST GET PUT DELETE GET404 and JDBC")
    void completeCrudScenarioMatchesCommittedDatabase() throws Exception {
        var post = send("POST", "/employees", employee("Day2Employee", "day2@example.test", 1), 201);
        var created = tree(post);
        long id = created.get("id").asLong();
        assertThat(post.headers().firstValue("location")).contains("/employees/" + id);
        assertThat(created.get("departmentName").asText()).isEqualTo("開発部");
        assertCommittedEmployee(id, "Day2Employee", "day2@example.test", 1);
        assertThat(tree(send("GET", "/employees/" + id, null, 200))).isEqualTo(created);

        var updated = tree(send("PUT", "/employees/" + id,
                employee("Day2Updated", "day2-updated@example.test", 2), 200));
        assertThat(updated.get("id").asLong()).isEqualTo(id);
        assertThat(updated.get("name").asText()).isEqualTo("Day2Updated");
        assertThat(updated.get("email").asText()).isEqualTo("day2-updated@example.test");
        assertThat(updated.get("departmentName").asText()).isEqualTo("営業部");
        assertCommittedEmployee(id, "Day2Updated", "day2-updated@example.test", 2);
        assertThat(tree(send("GET", "/employees/" + id, null, 200))).isEqualTo(updated);

        assertThat(send("DELETE", "/employees/" + id, null, 204).body()).isEmpty();
        assertThat(employeeCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM EMPLOYEE WHERE ID=?", Integer.class, id)).isZero();
        var missing = tree(send("GET", "/employees/" + id, null, 404));
        assertThat(missing.get("status").asInt()).isEqualTo(404);
        assertThat(missing.get("message").asText()).isNotBlank();
        assertThat(missing.get("errors").isObject()).isTrue();
        assertThat(departmentCount()).isEqualTo(2);
        System.out.println("[SPR-W7-D2-04] PASS POST201 -> GET200 -> PUT200 -> DELETE204 -> GET404");
        System.out.println("[SPR-W7-D2-04] PASS independent JDBC: created=1 updated=1 deleted=0; DEPARTMENT=2");
    }
}
