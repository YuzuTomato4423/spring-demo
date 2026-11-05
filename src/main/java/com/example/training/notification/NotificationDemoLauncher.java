package com.example.training.notification;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.example.training.notification.NotificationClients.MailQualified;
import com.example.training.notification.NotificationClients.NamedMail;
import com.example.training.notification.NotificationClients.TypeOnly;
import com.example.training.notification.NotificationConfigurations.SlackPrimary;
import com.example.training.notification.NotificationConfigurations.TwoBeans;

/** 通常アプリの走査範囲外に置いた実習入口。引数01～05でEclipseから各段階を再実行する。 */
public final class NotificationDemoLauncher {
    private NotificationDemoLauncher() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("課題番号01～05を1つ指定してください。");
        }
        String stage = args[0];
        System.out.println("=== G-1005-SPR / SPR-1005-" + stage + " : actual Spring execution ===");
        switch (stage) {
            case "01" -> conflict();
            case "02" -> qualified(TwoBeans.class);
            case "03" -> primary();
            case "04" -> qualified(SlackPrimary.class);
            case "05" -> finalResults();
            default -> throw new IllegalArgumentException("未対応の課題番号: " + stage);
        }
        System.out.println("=== SPR-1005-" + stage + " : PASS ===");
    }

    static AnnotationConfigApplicationContext open(Class<?> configuration, Class<?>... clients) {
        var context = new AnnotationConfigApplicationContext();
        try {
            context.register(configuration);
            context.register(clients);
            context.refresh();
            return context;
        } catch (RuntimeException error) {
            context.close();
            throw error;
        }
    }

    private static void conflict() {
        System.out.println("Beans: mailNotificationService / slackNotificationService");
        System.out.println("Injection: NotificationService by type; no Qualifier / no Primary");
        try (var context = open(TwoBeans.class, TypeOnly.class)) {
            throw new IllegalStateException("期待したBean競合が発生しませんでした。");
        } catch (BeansException error) {
            var conflict = findConflict(error);
            if (conflict == null || conflict.getNumberOfBeansFound() != 2) {
                throw error;
            }
            // Springが実際に発生させた例外を表示し、想定外の失敗と区別する。
            error.printStackTrace(System.out);
            System.out.println("EXPECTED CONFLICT: " + conflict.getClass().getSimpleName());
            System.out.println("Candidates: " + conflict.getBeanNamesFound());
        }
    }

    static NoUniqueBeanDefinitionException findConflict(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof NoUniqueBeanDefinitionException conflict) {
                return conflict;
            }
        }
        return null;
    }

    private static void qualified(Class<?> configuration) {
        try (var context = open(configuration, MailQualified.class)) {
            System.out.println("Primary: " + (configuration == SlackPrimary.class ? "slackNotificationService" : "none"));
            System.out.println("Injection: @Qualifier(\"mailNotificationService\")");
            report(context, "QUALIFIER", context.getBean(MailQualified.class).service(), "mailNotificationService");
        }
    }

    private static void primary() {
        try (var context = open(SlackPrimary.class, TypeOnly.class)) {
            System.out.println("Primary: slackNotificationService; injection without Qualifier");
            report(context, "PRIMARY", context.getBean(TypeOnly.class).service(), "slackNotificationService");
        }
    }

    private static void finalResults() {
        try (var context = open(SlackPrimary.class, TypeOnly.class, MailQualified.class, NamedMail.class)) {
            System.out.println("Registered NotificationService beans: " + context.getBeansOfType(NotificationService.class).keySet());
            report(context, "PRIMARY default", context.getBean(TypeOnly.class).service(), "slackNotificationService");
            report(context, "QUALIFIER explicit", context.getBean(MailQualified.class).service(), "mailNotificationService");
            report(context, "RESOURCE by name", context.getBean(NamedMail.class).service(), "mailNotificationService");
            System.out.println("Design comparison: docs/SPR-1005-design-comparison.md");
            System.out.println("Context closed automatically after each stage. External delivery: disabled (simulation).");
        }
    }

    private static void report(AnnotationConfigApplicationContext context, String selection,
            NotificationService injected, String expectedName) {
        NotificationService expected = context.getBean(expectedName, NotificationService.class);
        if (injected != expected) {
            throw new IllegalStateException("注入先が期待したBeanと一致しません: " + selection);
        }
        System.out.println("[" + selection + "] selected=" + injected.getClass().getSimpleName()
                + " bean=" + expectedName + " sameAsContext=true");
        System.out.println("Result: " + injected.send("社員登録完了 / employee registration completed"));
    }
}
