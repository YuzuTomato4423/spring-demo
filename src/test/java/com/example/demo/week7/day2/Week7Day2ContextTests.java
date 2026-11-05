package com.example.demo.week7.day2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import com.example.demo.controller.Week6Day3EmployeeController;
import com.example.demo.repository.EmployeeJpaRepository;
import com.example.demo.service.EmployeeBusinessService;
import com.example.demo.service.EmployeeJpaService;

/** SPR-W7-D2-01: 主要Beanを実際のContextから取得し、実HTTPから空の社員DBを参照する。 */
@DisplayName("SPR-W7-D2-01 SpringBootTest context")
class Week7Day2ContextTests extends Week7Day2IntegrationSupport {
    @Test
    @DisplayName("SPR-W7-D2-01 real beans and HTTP 200")
    void contextStartsWithRealApplicationBeans() throws Exception {
        assertThat(context.isActive()).isTrue();
        for (var type : new Class<?>[] {Week6Day3EmployeeController.class, EmployeeBusinessService.class,
                EmployeeJpaService.class, EmployeeJpaRepository.class, DataSource.class}) {
            var bean = context.getBean(type);
            assertThat(mockingDetails(bean).isMock()).as(type.getSimpleName()).isFalse();
            System.out.printf("[SPR-W7-D2-01] REAL BEAN %s%n", type.getSimpleName());
        }
        assertThat(AopUtils.isAopProxy(context.getBean(EmployeeBusinessService.class))).isTrue();
        var employees = tree(send("GET", "/employees?sort=id,asc", null, 200));
        assertThat(employees.isArray()).isTrue();
        assertThat(employees.size()).isZero();
        System.out.println("[SPR-W7-D2-01] PASS context active; Web/Service/Repository/H2 real beans; GET 200 []");
    }
}
