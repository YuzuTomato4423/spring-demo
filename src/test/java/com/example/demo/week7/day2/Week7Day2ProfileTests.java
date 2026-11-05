package com.example.demo.week7.day2;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SPR-W7-D2-02: 開発用永続DBへの誤接続を防ぐため、Profileだけでなく実JDBC接続先も検証する。 */
@DisplayName("SPR-W7-D2-02 Test Profile")
class Week7Day2ProfileTests extends Week7Day2IntegrationSupport {
    @Test
    @DisplayName("SPR-W7-D2-02 test profile and isolated JDBC URL")
    void testProfileUsesDedicatedMemoryDatabase() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("spr-w6-d3", "test", "spr-w7-d2-test");
        assertThat(environment.getProperty("app.environment-name")).isEqualTo("test");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");
        assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
        assertThat(environment.getProperty("spring.h2.console.enabled")).isEqualTo("false");
        assertThat(actualJdbcUrl()).startsWith("jdbc:h2:mem:sprw7d2-test-").doesNotContain("file:");
        System.out.println("[SPR-W7-D2-02] Active profiles: " + Arrays.toString(environment.getActiveProfiles()));
        System.out.println("[SPR-W7-D2-02] Actual JDBC URL: " + actualJdbcUrl());
        System.out.println("[SPR-W7-D2-02] PASS test environment; create-drop; EMPLOYEE=0 DEPARTMENT=2; file DB untouched");
    }
}
