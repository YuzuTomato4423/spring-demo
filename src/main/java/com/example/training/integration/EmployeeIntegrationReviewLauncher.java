package com.example.training.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;

import com.example.demo.SpringDemoApplication;
import com.example.demo.config.AppProperties;
import com.example.demo.controller.EmployeeController;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.model.Employee;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;
import com.example.training.notification.NotificationService;

/** 実Bootアプリの登録・通知・HTTP・終了を照合し、Eclipseから同じ検証を再現する。 */
public final class EmployeeIntegrationReviewLauncher {
    private EmployeeIntegrationReviewLauncher() { }

    public static void main(String[] args) throws Exception {
        EmployeeRegistrationNotifier notifier;
        Employee saved;
        String profile;
        try (var context = new SpringApplicationBuilder(SpringDemoApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .properties("server.address=127.0.0.1", "server.port=0", "spring.output.ansi.enabled=never")
                .run(args)) {
            var settings = context.getBean(AppProperties.class);
            var controller = context.getBean(EmployeeController.class);
            var service = context.getBean(EmployeeService.class);
            var repository = context.getBean(EmployeeRepository.class);
            notifier = context.getBean(EmployeeRegistrationNotifier.class);
            String[] active = context.getEnvironment().getActiveProfiles();
            profile = active.length == 0 ? "default" : String.join(",", active);
            boolean production = Arrays.asList(active).contains("prod");
            String expectedChannel = production ? "PROD_MAIL (simulation): " : "DEV_CONSOLE (simulation): ";
            check(notifier.isReady(), "初期化が利用前に完了していません。");
            check(context.getBeansOfType(NotificationService.class).keySet().equals(
                    Set.of("employeeRegistrationNotification", "slackNotificationService")), "通知候補が不正です。");
            check(context.isSingleton("employeeRegistrationNotifier")
                    && notifier == context.getBean(EmployeeRegistrationNotifier.class), "Scopeが不正です。");
            controller.register(100901, new EmployeeRequest("統合確認社員", 1));
            saved = service.findById(100901);
            check(saved == repository.findById(100901).orElseThrow(), "同じ保存先を参照していません。");
            check(service.findByDepartmentId(1).contains(saved), "部署検索が不正です。");
            check(notifier.getDeliveryCount() == 1 && notifier.getLastResult().equals(
                    expectedChannel + "[" + settings.getEnvironmentName() + "] registered employee id=100901"),
                    "登録後の通知が設定と一致しません。");
            try {
                controller.register(100902, new EmployeeRequest(" ", 1));
                throw new IllegalStateException("不正な社員名を登録できてしまいました。");
            } catch (IllegalArgumentException expected) {
                check(repository.findById(100902).isEmpty() && notifier.getDeliveryCount() == 1,
                        "失敗した登録を通知しています。");
            }
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            URI uri = URI.create("http://127.0.0.1:" + port + "/hello");
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build().send(
                    HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 200 && response.body().equals(settings.getMessage()),
                    "HTTP応答と外部設定が一致しません。");

            System.out.println("=== SPR-1009-01 / 02 / 03 : actual Spring Boot review ===");
            System.out.println("Profile=" + profile + " | app.environment-name=" + settings.getEnvironmentName());
            System.out.println("Lifecycle: initialized before registration=true");
            System.out.println("Qualifier: employeeRegistrationNotification selected from 2 candidates");
            System.out.println("Scope: notifier/service/repository=singleton; Employee/DTO=per registration");
            System.out.println("Registration -> same Repository -> department search: PASS");
            System.out.println("Notification: " + notifier.getLastResult());
            System.out.println("Invalid registration: rejected; saved=false; notification count unchanged=1");
            System.out.println("HTTP GET " + uri + " -> " + response.statusCode() + " | " + response.body());
        }
        check(!notifier.isReady(), "破棄コールバックが実行されていません。");
        try {
            notifier.notifyRegistration(saved);
            throw new IllegalStateException("終了後の通知が拒否されていません。");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().equals("通知Beanの準備が未完了、または終了済みです。"),
                    "終了後の通知検証に失敗しました。");
        }
        System.out.println("Lifecycle: Context closed; ready=false; use after destruction rejected");
        System.out.println("=== SPR-1009-01 / 02 / 03 : PASS (" + profile + ") ===");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
