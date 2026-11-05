package com.example.demo;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import com.example.demo.training.rollback.EmployeeRollbackController;
import com.example.demo.training.rollback.EmployeeRollbackExperiment;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:d4-normal-${random.uuid}","spring.h2.console.enabled=false"})
@ActiveProfiles({"spr-w5-d1","spr-w5-d2","spr-w5-d3","spr-w5-d4"})
class Week5Day4NormalProfileTests {
    @Autowired ApplicationContext context;
    @Test void ordinaryProfileDoesNotRegisterExperimentOrItsEndpoint() {
        assertThat(context.getBeansOfType(EmployeeRollbackController.class)).isEmpty();
        assertThat(context.getBeansOfType(EmployeeRollbackExperiment.class)).isEmpty();
    }
}
