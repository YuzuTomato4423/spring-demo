package com.example.demo.notification;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.demo.SpringDemoApplication;
import com.example.demo.config.AppProperties;
import com.example.demo.controller.EmployeeController;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.model.Department;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;
import com.example.training.integration.EmployeeIntegrationReviewLauncher;
import com.example.training.notification.NotificationService;

@ExtendWith(OutputCaptureExtension.class)
class EmployeeNotificationIntegrationTests {
    private SpringApplicationBuilder builder() {
        return new SpringApplicationBuilder(SpringDemoApplication.class).web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF).logStartupInfo(false).properties("logging.level.root=WARN");
    }

    @Test void profileSelectsQualifiedChannelAndPreservesSingletonRegistrationFlow() {
        for (String profile : new String[]{"", "dev", "prod"}) {
            String[] args = profile.isEmpty() ? new String[0] : new String[]{"--spring.profiles.active=" + profile};
            try (var context = builder().run(args)) {
                var notifier = context.getBean(EmployeeRegistrationNotifier.class);
                var service = context.getBean(EmployeeService.class);
                var controller = context.getBean(EmployeeController.class);
                var repository = context.getBean(EmployeeRepository.class);
                assertEquals(Set.of("employeeRegistrationNotification", "slackNotificationService"),
                        context.getBeansOfType(NotificationService.class).keySet());
                assertSame(context.getBean("employeeRegistrationNotification"), ReflectionTestUtils.getField(notifier, "channel"));
                assertSame(notifier, ReflectionTestUtils.getField(service, "notifier"));
                assertSame(notifier, context.getBean(EmployeeRegistrationNotifier.class));
                assertTrue(context.isSingleton("employeeRegistrationNotifier"));
                assertTrue(notifier.isReady());
                controller.register(901, new EmployeeRequest("テスト社員", 1));
                assertSame(repository.findById(901).orElseThrow(), service.findById(901));
                assertEquals(1, notifier.getDeliveryCount());
                String environment = profile.isEmpty() ? "default" : profile;
                String channel = profile.equals("prod") ? "PROD_MAIL" : "DEV_CONSOLE";
                assertEquals(channel + " (simulation): [" + environment + "] registered employee id=901",
                        notifier.getLastResult());
            }
        }
    }

    @Test void rejectedNameAndDepartmentNeverSaveOrNotify() {
        try (var context = builder().run()) {
            var controller = context.getBean(EmployeeController.class);
            var notifier = context.getBean(EmployeeRegistrationNotifier.class);
            var repository = context.getBean(EmployeeRepository.class);
            for (String name : new String[]{null, "", " "}) {
                assertThrows(IllegalArgumentException.class, () -> controller.register(902, new EmployeeRequest(name, 1)));
            }
            assertThrows(IllegalArgumentException.class, () -> controller.register(902, new EmployeeRequest("社員", 99)));
            assertTrue(repository.findAll().isEmpty());
            assertEquals(0, notifier.getDeliveryCount());
            assertNull(notifier.getLastResult());
        }
    }

    @Test void repositoryFailureDoesNotSendNotification() {
        var settings = new AppProperties();
        settings.setEnvironmentName("test");
        var calls = new AtomicInteger();
        var notifier = new EmployeeRegistrationNotifier(message -> { calls.incrementAndGet(); return message; }, settings);
        notifier.initialize();
        EmployeeRepository failedRepository = new EmployeeRepository() {
            public void save(Employee employee) { throw new IllegalStateException("保存失敗"); }
            public Employee insert(String name, Department department) { throw new IllegalStateException("保存失敗"); }
            public boolean replace(Employee employee) { throw new IllegalStateException("保存失敗"); }
            public boolean deleteById(int id) { throw new IllegalStateException("保存失敗"); }
            public Optional<Employee> findById(int id) { return Optional.empty(); }
            public List<Employee> findAll() { return List.of(); }
        };
        var service = new EmployeeService(failedRepository, notifier);
        assertEquals("保存失敗", assertThrows(IllegalStateException.class,
                () -> service.register(new Employee(903, "社員", new Department(1, "開発部")))).getMessage());
        assertEquals(0, calls.get());
        assertEquals(0, notifier.getDeliveryCount());
    }

    @Test void notifierRejectsUseBeforeInitialization() {
        var calls = new AtomicInteger();
        var notifier = new EmployeeRegistrationNotifier(message -> { calls.incrementAndGet(); return message; }, new AppProperties());
        assertThrows(IllegalStateException.class,
                () -> notifier.notifyRegistration(new Employee(904, "社員", new Department(1, "開発部"))));
        assertEquals(0, calls.get());
    }

    @Test void springClosesNotifierAndRejectsStaleReference(CapturedOutput output) {
        var context = builder().run("--logging.level.com.example.demo.notification=INFO");
        var notifier = context.getBean(EmployeeRegistrationNotifier.class);
        var employee = new Employee(905, "社員", new Department(1, "開発部"));
        assertTrue(notifier.isReady());
        notifier.notifyRegistration(employee);
        context.close();
        assertFalse(notifier.isReady());
        assertThrows(IllegalStateException.class, () -> notifier.notifyRegistration(employee));
        assertEquals(1, notifier.getDeliveryCount());
        String log = output.getAll();
        assertTrue(log.indexOf("[EMP-NOTIFY] INIT") < log.indexOf("registered employee id=905"));
        assertTrue(log.indexOf("registered employee id=905") < log.indexOf("[EMP-NOTIFY] DESTROY"));
    }

    @Test void externalEnvironmentOverrideAppearsInActualNotification() {
        try (var context = builder().run("--spring.profiles.active=dev", "--app.environment-name=review-override")) {
            context.getBean(EmployeeController.class).register(906, new EmployeeRequest("社員", 1));
            assertEquals("DEV_CONSOLE (simulation): [review-override] registered employee id=906",
                    context.getBean(EmployeeRegistrationNotifier.class).getLastResult());
        }
    }

    @Test void eclipseLauncherChecksRealHttpRegistrationAndDestructionForAllEnvironments(CapturedOutput output) throws Exception {
        for (String profile : new String[]{"default", "dev", "prod"}) {
            String[] args = profile.equals("default") ? new String[]{"--logging.level.root=WARN"}
                    : new String[]{"--spring.profiles.active=" + profile, "--logging.level.root=WARN"};
            EmployeeIntegrationReviewLauncher.main(args);
            assertTrue(output.getAll().contains("PASS (" + profile + ")"));
        }
        assertTrue(output.getAll().contains("use after destruction rejected"));
        assertTrue(output.getAll().contains("notification count unchanged=1"));
    }
}
