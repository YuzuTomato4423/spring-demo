package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.entity.EmployeeEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** SPR-W5-D1-01～04: 実H2で接続・DDL・JPA採番・再取得を確認する。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sprw5d1-test-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
        "spring.h2.console.enabled=false"
})
@ActiveProfiles("spr-w5-d1")
class Week5Day1JpaTests {
    @PersistenceContext private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test void employeeTableHasExpectedColumnsAndGeneratedPrimaryKey() {
        assertThat(jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'EMPLOYEE' "
                + "ORDER BY COLUMN_NAME", String.class))
                .containsExactly("DEPARTMENT_ID", "EMAIL", "ID", "NAME");
        // SPR-W6-D3-01: メール列追加後も過去課題のメールなし社員を保存できる。
        assertThat(jdbc.queryForObject("SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'EMPLOYEE' "
                + "AND COLUMN_NAME = 'EMAIL'", String.class)).isEqualTo("YES");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'EMPLOYEE' "
                + "AND CONSTRAINT_TYPE = 'PRIMARY KEY'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT IS_IDENTITY FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'EMPLOYEE' "
                + "AND COLUMN_NAME = 'ID'", String.class)).isEqualTo("YES");
    }

    @Test @Transactional void jpaGeneratesDistinctIdsAndReloadsSavedFields() {
        var first = new EmployeeEntity("JPA確認社員", entityManager.find(com.example.demo.entity.DepartmentEntity.class, 1));
        var second = new EmployeeEntity("採番確認社員", entityManager.find(com.example.demo.entity.DepartmentEntity.class, 2));
        assertThat(first.getId()).isNull();
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();
        Long firstId = first.getId();
        assertThat(firstId).isPositive();
        assertThat(second.getId()).isPositive().isNotEqualTo(firstId);
        entityManager.clear();
        var loaded = entityManager.find(EmployeeEntity.class, firstId);
        assertThat(loaded).isNotSameAs(first);
        assertThat(loaded.getName()).isEqualTo("JPA確認社員");
        assertThat(loaded.getEmail()).isNull();
        assertThat(loaded.getDepartmentId()).isEqualTo(1);
    }
}
