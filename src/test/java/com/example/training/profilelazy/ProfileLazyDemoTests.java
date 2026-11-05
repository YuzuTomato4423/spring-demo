package com.example.training.profilelazy;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import com.example.demo.SpringDemoApplication;
import com.example.demo.controller.EmployeeController;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.service.EmployeeService;
import com.example.training.notification.NotificationService;
import com.example.training.profilelazy.ProfileLazyConfigurations.*;

class ProfileLazyDemoTests {
    @Test void profilesSelectExactlyOneActualInjectedBean() {
        for (String profile : List.of("dev", "prod")) {
            try (var context = ProfileLazyDemoLauncher.open(profile, null, false)) {
                var beans = context.getBeansOfType(NotificationService.class);
                assertEquals(1, beans.size());
                NotificationService service = context.getBean(NotificationClient.class).service();
                assertSame(beans.values().iterator().next(), service);
                assertEquals(profile.equals("dev") ? DevNotificationService.class : ProdNotificationService.class,
                        service.getClass());
                assertEquals((profile.equals("dev") ? "DEV_CONSOLE" : "PROD_MAIL") + " (simulation): test",
                        service.send("test"));
                assertFalse(context.containsBean(profile.equals("dev") ? "prodNotificationService" : "devNotificationService"));
            }
        }
    }

    @Test void missingUnknownOrCombinedProfilesFailClearly() {
        for (String invalid : new String[] { null, "", " ", "qa", "dev,prod", "prod,dev" }) {
            assertThrows(IllegalArgumentException.class, () -> ProfileLazyDemoLauncher.validateProfile(invalid));
        }
    }

    @Test void lazyCreationOccursOnlyAfterFirstRequestAndOnlyOnce() {
        try (var context = ProfileLazyDemoLauncher.open("dev", true, false)) {
            InitializationProbe probe = context.getBean(InitializationProbe.class);
            assertEquals(0, probe.createdCount());
            assertFalse(context.getBeanFactory().containsSingleton("heavyEmployeeReport"));
            HeavyEmployeeReport first = ProfileLazyDemoLauncher.acquire(context, probe);
            assertSame(first, context.getBean(HeavyEmployeeReport.class));
            assertEquals(1, probe.createdCount());
            assertEquals(1, probe.initializedCount());
            assertEquals(List.of("STARTUP_COMPLETE", "FIRST_GET_REQUEST", "CONSTRUCTOR",
                    "INIT_BEGIN simulatedCost=1000ms", "INIT_DONE", "FIRST_GET_COMPLETE", "SECOND_GET_COMPLETE"), probe.events());
        }
    }

    @Test void eagerInitializationCompletesBeforeStartupAndIsNotRepeated() {
        try (var context = ProfileLazyDemoLauncher.open("dev", false, false)) {
            InitializationProbe probe = context.getBean(InitializationProbe.class);
            assertTrue(context.getBeanFactory().containsSingleton("heavyEmployeeReport"));
            assertEquals(1, probe.initializedCount());
            ProfileLazyDemoLauncher.acquire(context, probe);
            assertEquals(List.of("CONSTRUCTOR", "INIT_BEGIN simulatedCost=1000ms", "INIT_DONE",
                    "STARTUP_COMPLETE", "FIRST_GET_REQUEST", "FIRST_GET_COMPLETE", "SECOND_GET_COMPLETE"), probe.events());
        }
    }

    @Test void employeeRegistrationNotificationAndOptionalReportWorkTogether() {
        var context = ProfileLazyDemoLauncher.open("prod", true, true);
        try (context) {
            InitializationProbe probe = context.getBean(InitializationProbe.class);
            context.getBean(EmployeeController.class).register(1006, new EmployeeRequest("Profile Lazy Sample", 1));
            var service = context.getBean(EmployeeService.class);
            assertEquals("Profile Lazy Sample", service.findById(1006).getName());
            assertEquals(0, probe.createdCount());
            assertEquals("PROD_MAIL (simulation): registered",
                    context.getBean(NotificationClient.class).service().send("registered"));
            assertEquals("Employee report: count=1 ids=[1006]",
                    ProfileLazyDemoLauncher.acquire(context, probe).generate(service.findAll()));
        }
        assertFalse(context.isActive());
    }

    @Test void ordinaryApplicationWithEitherProfileKeepsOptionalTrainingBeansIsolated() {
        for (String profile : List.of("dev", "prod")) {
            try (var context = new SpringApplicationBuilder(SpringDemoApplication.class)
                    .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                    .profiles(profile).properties("logging.level.root=WARN").run()) {
                assertEquals(java.util.Set.of("employeeRegistrationNotification", "slackNotificationService"),
                        context.getBeansOfType(NotificationService.class).keySet());
                assertTrue(context.getBeansOfType(NotificationClient.class).isEmpty());
                assertTrue(context.getBeansOfType(HeavyEmployeeReport.class).isEmpty());
                assertTrue(context.getBeansOfType(InitializationProbe.class).isEmpty());
            }
        }
    }
}
