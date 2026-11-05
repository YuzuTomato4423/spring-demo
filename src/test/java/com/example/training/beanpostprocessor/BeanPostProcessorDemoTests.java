package com.example.training.beanpostprocessor;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.example.demo.SpringDemoApplication;
import com.example.demo.HelloController;
import com.example.demo.controller.EmployeeController;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;

@ExtendWith(OutputCaptureExtension.class)
class BeanPostProcessorDemoTests {
    @Test
    void broadAndRestrictedContextsHaveRealCallbacksAndPreserveDiAndOperations() {
        for (Class<?> config : List.of(BeanPostProcessorConfigurations.AllBeans.class,
                BeanPostProcessorConfigurations.EmployeeOnly.class)) {
            try (var context = BeanPostProcessorDemoLauncher.open(config)) {
                var observer = context.getBean(EmployeeBeanPostProcessor.class);
                for (Class<?> type : List.of(EmployeeController.class, EmployeeService.class,
                        EmployeeMapper.class, EmployeeRepository.class)) {
                    Object bean = context.getBean(type);
                    var events = observer.events().stream().filter(e -> e.bean() == bean).toList();
                    assertEquals(2, events.size());
                    assertEquals(List.of("BEFORE", "AFTER"), events.stream().map(EmployeeBeanPostProcessor.Event::phase).toList());
                }
                var service = context.getBean(EmployeeService.class);
                var repository = context.getBean(EmployeeRepository.class);
                var controller = context.getBean(EmployeeController.class);
                assertSame(repository, ReflectionTestUtils.getField(service, "repository"));
                assertSame(service, ReflectionTestUtils.getField(controller, "service"));
                controller.register(701, new EmployeeRequest("実測社員", 1));
                assertSame(repository.findById(701).orElseThrow(), service.findById(701));
                assertEquals("実測社員", service.findById(701).getName());
                assertEquals(!observer.isEmployeeOnly(), observer.events().stream().anyMatch(e -> e.bean() instanceof HelloController));
                if (observer.isEmployeeOnly()) {
                    assertTrue(observer.events().stream().allMatch(e -> EmployeeBeanPostProcessor.employeeRelated(e.bean())));
                    assertTrue(observer.events().stream().anyMatch(e -> e.beanName().equals("employeeFormatter")));
                    assertTrue(observer.events().stream().anyMatch(e -> e.beanName().equals("formatterHolder")));
                    assertTrue(observer.events().stream().anyMatch(e -> e.bean() instanceof EmployeeRepository));
                }
                int before = observer.events().size();
                context.getBean(EmployeeService.class);
                assertEquals(before, observer.events().size(), "singletonの再取得で初期化callbackを繰り返さない");
            }
        }
    }

    @Test
    void callbacksRunInDiagramOrderAndCloseDestroysOnce() {
        var context = BeanPostProcessorDemoLauncher.open(BeanPostProcessorConfigurations.Lifecycle.class);
        var observer = context.getBean(EmployeeBeanPostProcessor.class);
        var probe = context.getBean(EmployeeLifecycleProbe.class);
        try (context) {
            assertSame(context.getBean(EmployeeService.class), probe.service());
            assertTrue(probe.isInitialized());
            probe.use();
            observer.lifecycle("07 CONTEXT_CLOSE_BEGIN");
        }
        context.close();
        observer.lifecycle("09 CONTEXT_CLOSED active=" + context.isActive());
        assertEquals(9, observer.lifecycleEvents().size());
        for (int i = 0; i < 9; i++) assertTrue(observer.lifecycleEvents().get(i).startsWith(String.format("%02d ", i + 1)));
        assertTrue(observer.lifecycleEvents().get(1).contains("initialized=false"));
        assertTrue(observer.lifecycleEvents().get(4).contains("initialized=true"));
        assertTrue(probe.isDestroyed());
        assertFalse(probe.isInitialized());
        assertThrows(IllegalStateException.class, probe::use);
    }

    @Test
    void normalBootApplicationDoesNotActivateTrainingProcessor() {
        try (var context = new SpringApplicationBuilder(SpringDemoApplication.class)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false).run()) {
            assertTrue(context.getBeansOfType(EmployeeBeanPostProcessor.class).isEmpty());
            assertTrue(context.getBeansOfType(EmployeeLifecycleProbe.class).isEmpty());
        }
    }

    @Test
    void eclipseEntryPointsProduceAllRequiredResults(CapturedOutput output) {
        for (String stage : List.of("01", "02", "03", "04")) BeanPostProcessorDemoLauncher.main(new String[]{stage});
        for (String stage : List.of("01", "02", "03", "04")) assertTrue(output.getOut().contains("SPR-1007-"+stage+" : PASS"));
        assertTrue(output.getOut().contains("HelloController logged=false"));
        assertTrue(output.getOut().contains("ConditionalOnMissingBean"));
        assertTrue(output.getOut().contains("[LIFE 02 BPP_BEFORE initialized=false]"));
        assertTrue(output.getOut().contains("[LIFE 05 BPP_AFTER initialized=true]"));
        assertTrue(output.getOut().contains("Lifecycle diagram"));
    }

    @Test
    void invalidInvocationAndUnavailableResourceFailClearly() {
        for (String[] args : List.of(new String[0], new String[]{"05"}, new String[]{"01","02"})) {
            assertThrows(IllegalArgumentException.class, () -> BeanPostProcessorDemoLauncher.main(args));
        }
        assertThrows(IllegalStateException.class, () -> BeanPostProcessorDemoLauncher.readResource("not-found.txt"));
    }
}
