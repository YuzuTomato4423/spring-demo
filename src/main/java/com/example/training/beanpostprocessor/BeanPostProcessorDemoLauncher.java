package com.example.training.beanpostprocessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.example.demo.HelloController;
import com.example.demo.controller.EmployeeController;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;

/** SPR-1007-01～04の再実行入口。実測・検証に成功した場合だけPASSを表示する。 */
public final class BeanPostProcessorDemoLauncher {
    private BeanPostProcessorDemoLauncher() { }

    public static void main(String[] args) {
        if (args.length != 1 || !List.of("01", "02", "03", "04").contains(args[0])) {
            throw new IllegalArgumentException("課題番号01～04を1つ指定してください。");
        }
        String stage = args[0];
        System.out.println("=== G-1007-SPR / SPR-1007-" + stage + " ===");
        switch (stage) {
            case "01" -> initialization(false);
            case "02" -> initialization(true);
            case "03" -> System.out.print(readResource("spr1007-auto-configuration.txt"));
            case "04" -> lifecycle();
            default -> throw new IllegalArgumentException(stage);
        }
        System.out.println("=== SPR-1007-" + stage + " : PASS ===");
    }

    static AnnotationConfigApplicationContext open(Class<?> configuration) {
        var context = new AnnotationConfigApplicationContext();
        try {
            context.register(configuration);
            context.refresh();
            return context;
        } catch (RuntimeException error) {
            context.close();
            throw error;
        }
    }

    private static void initialization(boolean restricted) {
        System.out.println("Log target: " + (restricted ? "employee-related types only" : "all beans"));
        try (var context = open(restricted ? BeanPostProcessorConfigurations.EmployeeOnly.class
                : BeanPostProcessorConfigurations.AllBeans.class)) {
            var observer = context.getBean(EmployeeBeanPostProcessor.class);
            for (Class<?> type : List.of(EmployeeController.class, EmployeeService.class,
                    EmployeeMapper.class, EmployeeRepository.class)) {
                Object bean = context.getBean(type);
                var events = observer.events().stream().filter(e -> e.bean() == bean).toList();
                check(events.size() == 2 && events.get(0).phase().equals("BEFORE")
                        && events.get(1).phase().equals("AFTER"), "before/afterが期待どおりではありません: " + type);
            }
            boolean helloLogged = observer.events().stream().anyMatch(e -> e.bean() instanceof HelloController);
            check(helloLogged != restricted, "対象外Beanの除外結果が不正です。");
            if (restricted) check(observer.events().stream().allMatch(e -> EmployeeBeanPostProcessor.employeeRelated(e.bean())),
                    "employee以外のBeanがログ対象です。");
            System.out.println("[CHECK] before/after=once per target; sameBean=true");
            System.out.println("[CHECK] HelloController logged=" + helloLogged);
            checkEmployeeOperation(context);
        }
    }

    private static void checkEmployeeOperation(AnnotationConfigApplicationContext context) {
        var controller = context.getBean(EmployeeController.class);
        var service = context.getBean(EmployeeService.class);
        var repository = context.getBean(EmployeeRepository.class);
        controller.register(1007, new EmployeeRequest("BPP確認社員", 1));
        check(service.findById(1007) == repository.findById(1007).orElseThrow(), "DI・社員保存先が不一致です。");
        check(service.findById(1007).getName().equals("BPP確認社員"), "社員登録結果が不正です。");
        System.out.println("[CHECK] employee registration/read=OK sameRepository=true");
    }

    private static void lifecycle() {
        var context = open(BeanPostProcessorConfigurations.Lifecycle.class);
        var observer = context.getBean(EmployeeBeanPostProcessor.class);
        var probe = context.getBean(EmployeeLifecycleProbe.class);
        try (context) {
            probe.use();
            observer.lifecycle("07 CONTEXT_CLOSE_BEGIN");
        }
        observer.lifecycle("09 CONTEXT_CLOSED active=" + context.isActive());
        List<String> markers = observer.lifecycleEvents();
        check(markers.size() == 9, "Lifecycleのイベント数が不正です。");
        for (int i = 0; i < markers.size(); i++) {
            check(markers.get(i).startsWith(String.format("%02d ", i + 1)), "Lifecycle順序が不正です。");
        }
        check(probe.isDestroyed() && !probe.isInitialized() && !context.isActive(), "破棄状態が不正です。");
        System.out.println("[CHECK] lifecycle=01..09 in order, destroyed=true");
        System.out.println(readResource("spr1007-lifecycle-diagram.txt"));
    }

    static String readResource(String name) {
        try (var input = BeanPostProcessorDemoLauncher.class.getResourceAsStream("/" + name)) {
            if (input == null) throw new IllegalStateException("必要な概念メモがありません: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("概念メモを読み取れません: " + name, error);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
