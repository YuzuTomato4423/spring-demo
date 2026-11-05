package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.demo.controller.EmployeeController;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.model.Department;
import com.example.demo.model.Employee;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;

/**
 * SPR-CARRY-01～03の実行証跡。Springから受け取ったBeanで旧Mainのデモを実行する。
 * carry-demoプロファイル限定なので、通常起動や通常のテストではサンプル登録をしない。
 */
@Component
@Profile("carry-demo & !spr-w5-d2")
public class CarryDemoRunner implements CommandLineRunner {
    private final ApplicationContext context;
    private final EmployeeController controller;
    private final EmployeeService service;
    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;

    public CarryDemoRunner(ApplicationContext context, EmployeeController controller,
            EmployeeService service, EmployeeRepository repository, EmployeeMapper mapper) {
        // Runner自体もBeanなので、確認対象のBeanをコンストラクタで注入できる。
        this.context = context;
        this.controller = controller;
        this.service = service;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== SPR-CARRY-01: Spring Bean ===");
        verifyBean(EmployeeController.class, controller);
        verifyBean(EmployeeService.class, service);
        verifyBean(EmployeeRepository.class, repository);
        verifyBean(EmployeeMapper.class, mapper);

        System.out.println("=== SPR-CARRY-02: DI / singleton ===");
        // constructorの[DI]ログにある識別値を、上のContext内の識別値と照合する。
        System.out.println("DI chain: " + identity(controller) + " -> "
                + identity(service) + " -> " + identity(repository));
        EmployeeService service2 = context.getBean(EmployeeService.class);
        System.out.println("service1 == service2 : " + (service == service2));
        System.out.println("service1 hashCode : " + service.hashCode());
        System.out.println("service2 hashCode : " + service2.hashCode());
        System.out.println("before counter : " + service.getCounter() + " / " + service2.getCounter());
        service.incrementCounter();
        System.out.println("after increment : " + service.getCounter() + " / " + service2.getCounter());

        // EmployeeやDTOはBeanではなく、登録ごとに作る業務データ。
        Department department = new Department(1, "Sales");
        EmployeeResponse response = mapper.toResponse(new Employee(100, "Tanaka", department));
        System.out.println("DTO conversion : " + response);
        String[] names = { "田中", "佐藤", "鈴木", "高橋", "伊藤", "山田" };
        for (int index = 0; index < names.length; index++) {
            int departmentId = (index == 1 || index == 3) ? 2 : 1;
            // Controller→Service→Repositoryを通して保存し、直後の読み取りでDI成立を確認する。
            controller.register(index + 1, new EmployeeRequest(names[index], departmentId));
        }
        if (repository.findAll().size() != 6 || !service.findById(6).getName().equals("山田")) {
            throw new IllegalStateException("社員登録のDI確認に失敗しました。");
        }
        System.out.println("Controller registration -> same Repository: PASS (6 employees)");

        System.out.println("=== SPR-CARRY-03: final results ===");
        System.out.println("社員一覧 : " + service.findAll());
        System.out.println("ID検索 (6) : " + service.findById(6));
        System.out.println("部署検索 (1) : " + service.findByDepartmentId(1));
        System.out.println("SPR-CARRY-01 / 02 / 03 : PASS");
    }

    private <T> void verifyBean(Class<T> type, T injected) {
        // getBeanは証跡の照合に限定し、業務処理はコンストラクタ注入した依存先を使う。
        var beans = context.getBeansOfType(type);
        if (beans.size() != 1 || context.getBean(type) != injected) {
            throw new IllegalStateException("Beanの一意性・注入参照が不正です: " + type.getSimpleName());
        }
        String name = beans.keySet().iterator().next();
        System.out.println("[BEAN] " + name + " = " + identity(injected)
                + " | count=1, sameAsContext=true, singleton=" + context.isSingleton(name));
    }

    private static String identity(Object bean) {
        return bean.getClass().getSimpleName() + "@"
                + Integer.toHexString(System.identityHashCode(bean));
    }
}
