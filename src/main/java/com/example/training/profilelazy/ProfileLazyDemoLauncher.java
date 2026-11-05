package com.example.training.profilelazy;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.example.demo.controller.EmployeeController;
import com.example.demo.config.AppProperties;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.repository.InMemoryEmployeeRepository;
import com.example.demo.service.EmployeeService;
import com.example.training.notification.NotificationService;
import com.example.training.profilelazy.ProfileLazyConfigurations.*;

/** SPR-1006-01～05。各実習を実際のSpring Contextで実行し、検証成功後だけPASSを表示する。 */
public final class ProfileLazyDemoLauncher {
    private ProfileLazyDemoLauncher() { }

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2 || !List.of("01", "02", "03", "04", "05").contains(args[0])) {
            throw new IllegalArgumentException("課題番号01～05を指定してください。04だけ比較対象eager/lazyを追加指定できます。");
        }
        String stage = args[0];
        if (args.length == 2 && (!stage.equals("04") || !List.of("eager", "lazy").contains(args[1]))) {
            throw new IllegalArgumentException("比較対象は課題04のeager/lazyだけです。");
        }
        String profile = validateProfile(System.getProperty("spring.profiles.active"));
        String expectedProfile = stage.equals("02") || stage.equals("05") ? "prod" : "dev";
        check(profile.equals(expectedProfile), "この起動設定のProfileは" + expectedProfile + "にしてください。");
        System.out.println("=== G-1006-SPR / SPR-1006-" + stage + " : actual Spring execution ===");
        System.out.println("Active profile: " + profile + " (VM: -Dspring.profiles.active=" + profile + ")");
        switch (stage) {
            case "01", "02" -> profileDemo(profile);
            case "03" -> reportDemo(profile, true);
            case "04" -> {
                if (args.length == 1 || args[1].equals("eager")) reportDemo(profile, false);
                if (args.length == 1 || args[1].equals("lazy")) reportDemo(profile, true);
            }
            case "05" -> employeeDemo(profile);
            default -> throw new IllegalArgumentException(stage);
        }
        System.out.println("=== SPR-1006-" + stage + " : PASS ===");
    }

    static String validateProfile(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("-Dspring.profiles.active=dev または prod を指定してください。");
        }
        String profile = value.trim();
        if (!profile.equals("dev") && !profile.equals("prod")) {
            throw new IllegalArgumentException("Profileはdev/prodのどちらか1つだけ指定してください: " + value);
        }
        return profile;
    }

    static AnnotationConfigApplicationContext open(String profile, Boolean lazy, boolean employees) {
        validateProfile(profile);
        var context = new AnnotationConfigApplicationContext();
        try {
            // refresh前に環境を確定する。既存のapplication.propertiesや通常起動には影響しない。
            context.getEnvironment().setActiveProfiles(profile);
            context.register(Notifications.class);
            if (lazy != null) {
                context.registerBean("initializationProbe", InitializationProbe.class);
                context.register(lazy ? LazyReport.class : EagerReport.class);
            }
            if (employees) {
                // この実習の通知候補は1つに保ち、統合ServiceのQualifierには別名で対応する。
                context.registerAlias(profile.equals("dev") ? "devNotificationService" : "prodNotificationService",
                        "employeeRegistrationNotification");
                context.registerBean(AppProperties.class, () -> {
                    var properties = new AppProperties();
                    properties.setEnvironmentName(profile);
                    return properties;
                });
                context.register(EmployeeController.class, EmployeeService.class,
                        EmployeeMapper.class, InMemoryEmployeeRepository.class, EmployeeRegistrationNotifier.class);
            }
            context.refresh();
            if (lazy != null) context.getBean(InitializationProbe.class).record("STARTUP_COMPLETE");
            return context;
        } catch (RuntimeException error) {
            context.close();
            throw error;
        }
    }

    private static NotificationService notification(AnnotationConfigApplicationContext context, String profile) {
        var beans = context.getBeansOfType(NotificationService.class);
        String expectedName = profile.equals("dev") ? "devNotificationService" : "prodNotificationService";
        check(beans.size() == 1 && beans.containsKey(expectedName), "通知Beanの選択が不正です。");
        NotificationService service = context.getBean(NotificationClient.class).service();
        check(service == beans.get(expectedName), "利用側へのDIがContextのBeanと一致しません。");
        System.out.println("[PROFILE] active=" + Arrays.toString(context.getEnvironment().getActiveProfiles())
                + " bean=" + expectedName + " implementation=" + service.getClass().getSimpleName());
        System.out.println("[PROFILE] beanCount=1 sameAsContext=true externalDelivery=simulation");
        return service;
    }

    private static void profileDemo(String profile) {
        var context = open(profile, null, false);
        try (context) {
            System.out.println("[NOTIFY] " + notification(context, profile).send("employee registration completed"));
        }
        check(!context.isActive(), "Contextが閉じられていません。");
        System.out.println("[CONTEXT] closed=true");
    }

    private static void reportDemo(String profile, boolean lazy) {
        System.out.println("--- Report comparison: " + (lazy ? "@Lazy present" : "@Lazy absent") + " ---");
        long beforeStartup = System.nanoTime();
        var context = open(profile, lazy, false);
        try (context) {
            long startupMs = elapsed(beforeStartup);
            InitializationProbe probe = context.getBean(InitializationProbe.class);
            boolean instantiated = context.getBeanFactory().containsSingleton("heavyEmployeeReport");
            check(instantiated == !lazy && probe.createdCount() == (lazy ? 0 : 1), "起動時の生成状態が不正です。");
            check(probe.initializedCount() == (lazy ? 0 : 1), "起動時の初期化回数が不正です。");
            System.out.println("[CHECK] startup created=" + probe.createdCount() + " singletonPresent=" + instantiated);
            HeavyEmployeeReport report = acquire(context, probe);
            System.out.println("[RESULT] " + report.generate(List.of()));
            check(probe.events().indexOf("INIT_DONE") < probe.events().indexOf("STARTUP_COMPLETE") == !lazy,
                    "初期化順序が期待と異なります。");
            System.out.println("[TIMING] startup=" + startupMs + "ms (reference only; assertions use order/count)");
        }
        check(!context.isActive(), "Contextが閉じられていません。");
        System.out.println("[CONTEXT] closed=true");
    }

    static HeavyEmployeeReport acquire(AnnotationConfigApplicationContext context, InitializationProbe probe) {
        probe.record("FIRST_GET_REQUEST");
        long before = System.nanoTime();
        HeavyEmployeeReport report = context.getBean(HeavyEmployeeReport.class);
        long firstGetMs = elapsed(before);
        probe.record("FIRST_GET_COMPLETE");
        check(probe.createdCount() == 1 && probe.initializedCount() == 1, "初回取得で初期化が完了していません。");
        HeavyEmployeeReport second = context.getBean(HeavyEmployeeReport.class);
        probe.record("SECOND_GET_COMPLETE");
        check(report == second && probe.createdCount() == 1 && probe.initializedCount() == 1, "再取得で生成が重複しました。");
        System.out.println("[CHECK] first/second get created=1 initialized=1 sameInstance=true firstGet=" + firstGetMs + "ms");
        return report;
    }

    private static void employeeDemo(String profile) {
        var context = open(profile, true, true);
        try (context) {
            NotificationService notification = notification(context, profile);
            var controller = context.getBean(EmployeeController.class);
            var service = context.getBean(EmployeeService.class);
            var mapper = context.getBean(EmployeeMapper.class);
            InitializationProbe probe = context.getBean(InitializationProbe.class);
            check(probe.createdCount() == 0, "社員登録前に任意レポートが生成されています。");
            controller.register(1006, new EmployeeRequest("Profile Lazy Sample", 1));
            var employee = service.findById(1006);
            check(employee.getName().equals("Profile Lazy Sample") && employee.getDepartment().getId() == 1,
                    "社員登録・取得結果が不正です。");
            System.out.println("[EMPLOYEE] registered/read=" + mapper.toResponse(employee));
            System.out.println("[NOTIFY] " + context.getBean(EmployeeRegistrationNotifier.class).getLastResult());
            check(probe.createdCount() == 0, "通常の社員処理が任意レポートを初期化しています。");
            System.out.println("[CHECK] after registration: optional report created=0");
            HeavyEmployeeReport report = acquire(context, probe);
            System.out.println("[RESULT] " + report.generate(service.findAll()));
            System.out.println("[DESIGN] environment-specific notification / lazy optional report / eager core employee beans");
            System.out.println("[DESIGN] rationale and comparison: docs/SPR-1006-README.md");
        }
        check(!context.isActive(), "Contextが閉じられていません。");
        System.out.println("[CONTEXT] closed=true");
    }

    private static long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
