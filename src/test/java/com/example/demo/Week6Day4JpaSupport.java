package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.demo.entity.DepartmentEntity;
import com.example.demo.repository.DepartmentJpaRepository;
import com.example.demo.repository.EmployeeJpaRepository;
import jakarta.persistence.EntityManager;

/** SPR-W6-D4-01/02: Web/Serviceを起動せず、JPAの実装・関連・Queryを実H2で検証する。 */
@DataJpaTest(showSql = false, properties = {
    "spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.h2.console.enabled=false"
})
@ActiveProfiles("spr-w5-d2")
abstract class Week6Day4JpaSupport {
    @Autowired EmployeeJpaRepository employees;
    @Autowired DepartmentJpaRepository departments;
    @Autowired EntityManager entityManager;
    DepartmentEntity development;
    DepartmentEntity sales;

    @BeforeEach void prepareDepartmentsInRollbackTransaction() {
        development = departments.save(new DepartmentEntity(1, "開発部"));
        sales = departments.save(new DepartmentEntity(2, "営業部"));
        // @DataJpaTestの各テスト終了時に社員・部署を一緒にrollbackする。
    }

    void flushAndClear() {
        employees.flush();
        entityManager.clear();
    }
}
