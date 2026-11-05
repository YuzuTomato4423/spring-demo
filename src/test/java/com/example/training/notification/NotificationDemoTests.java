package com.example.training.notification;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import com.example.demo.SpringDemoApplication;
import com.example.training.notification.NotificationClients.*;
import com.example.training.notification.NotificationConfigurations.*;

class NotificationDemoTests {
    @Test
    void typeOnlyInjectionFailsWithTheTwoActualCandidates() {
        var error = assertThrows(BeansException.class,
                () -> NotificationDemoLauncher.open(TwoBeans.class, TypeOnly.class));
        var conflict = NotificationDemoLauncher.findConflict(error);
        assertNotNull(conflict);
        assertEquals(2, conflict.getNumberOfBeansFound());
        assertEquals(Set.of("mailNotificationService", "slackNotificationService"),
                Set.copyOf(conflict.getBeanNamesFound()));
    }

    @Test
    void qualifierInjectsMailWithoutPrimary() {
        try (var context = NotificationDemoLauncher.open(TwoBeans.class, MailQualified.class)) {
            var injected = context.getBean(MailQualified.class).service();
            assertSame(context.getBean("mailNotificationService"), injected);
            assertEquals("MAIL: test", injected.send("test"));
            assertEquals(2, context.getBeansOfType(NotificationService.class).size());
        }
    }

    @Test
    void primarySelectsSlackWithoutQualifier() {
        try (var context = NotificationDemoLauncher.open(SlackPrimary.class, TypeOnly.class)) {
            var injected = context.getBean(TypeOnly.class).service();
            assertSame(context.getBean("slackNotificationService"), injected);
            assertEquals("SLACK: test", injected.send("test"));
        }
    }

    @Test
    void explicitMailQualifierAndBeanNameSelectionWorkAlongsideSlackPrimary() {
        try (var context = NotificationDemoLauncher.open(SlackPrimary.class,
                TypeOnly.class, MailQualified.class, NamedMail.class)) {
            assertSame(context.getBean("slackNotificationService"), context.getBean(TypeOnly.class).service());
            assertSame(context.getBean("mailNotificationService"), context.getBean(MailQualified.class).service());
            assertSame(context.getBean(MailQualified.class).service(), context.getBean(NamedMail.class).service());
        }
    }

    @Test
    void ordinaryApplicationRegistersOnlyIntegratedChannelsWithoutTrainingClients() {
        try (var context = new SpringApplicationBuilder(SpringDemoApplication.class)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                .properties("logging.level.root=WARN").run()) {
            assertEquals(Set.of("employeeRegistrationNotification", "slackNotificationService"),
                    context.getBeansOfType(NotificationService.class).keySet());
            assertTrue(context.getBeansOfType(TypeOnly.class).isEmpty());
            assertTrue(context.getBeansOfType(MailQualified.class).isEmpty());
        }
    }
}
