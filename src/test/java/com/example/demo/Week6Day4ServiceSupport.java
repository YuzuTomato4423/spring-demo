package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.example.demo.dto.BusinessEmployeeRequest;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.service.EmployeeBusinessService;

/**
 * SPR-W6-D4-03/04: SpringのService Proxyを直接呼び、HTTPを介さず業務ルールを確認する。
 * テスト全体を@Transactionalで囲まず、Service復帰後に別接続のJDBCでcommit結果を確認する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
    "spring.datasource.url=jdbc:h2:mem:sprw6d4-${random.uuid};DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
    "spring.h2.console.enabled=false", "spring.jpa.show-sql=false"
})
@ActiveProfiles("spr-w6-d3")
abstract class Week6Day4ServiceSupport {
    @Autowired EmployeeBusinessService service;
    @Autowired EmployeeRegistrationNotifier notifier;
    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach void clearOnlyDedicatedMemoryDatabase() throws Exception {
        // 誤ったProfileで既存DBを消さないよう、実接続URLを確認してから初期化する。
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:sprw6d4-");
        }
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        jdbc.update("DELETE FROM EMPLOYEE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DEPARTMENT", Integer.class)).isEqualTo(2);
    }

    BusinessEmployeeRequest request(String name, String email, int departmentId) {
        return new BusinessEmployeeRequest(name, email, departmentId);
    }

    List<Map<String, Object>> rows() {
        return jdbc.queryForList("SELECT ID,NAME,EMAIL,DEPARTMENT_ID FROM EMPLOYEE ORDER BY ID");
    }
}
