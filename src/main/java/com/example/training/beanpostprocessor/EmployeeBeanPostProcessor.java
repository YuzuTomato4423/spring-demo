package com.example.training.beanpostprocessor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;

import com.example.demo.EmployeeFormatter;
import com.example.demo.FormatterHolder;
import com.example.demo.controller.EmployeeController;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.service.EmployeeService;

/** SPR-1007-01/02: 各Beanに同じログ処理を適用する横断処理。業務メソッドやBeanの値は変更しない。 */
public final class EmployeeBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {
    public record Event(String phase, String beanName, Object bean) { }
    private final boolean employeeOnly;
    private final List<Event> events = new ArrayList<>();
    private final List<String> lifecycle = new ArrayList<>();

    public EmployeeBeanPostProcessor(boolean employeeOnly) {
        this.employeeOnly = employeeOnly;
    }

    @Override
    public int getOrder() {
        // SPR-1007-04: @PostConstructを処理するCommonAnnotationBeanPostProcessorより先にbeforeを実行する。
        // beforeチェーン内での順序を明示し、「before」という名前だけで初期化順を決めつけない。
        return HIGHEST_PRECEDENCE;
    }

    static boolean employeeRelated(Object bean) {
        // Bean名のprefixだけではinMemoryEmployeeRepositoryやformatterHolderを漏らすため型で限定する。
        // DTO/modelは通常のJavaオブジェクトであり、この実習ではSpring管理Beanとして登録しない。
        return bean instanceof EmployeeController || bean instanceof EmployeeService
                || bean instanceof EmployeeMapper || bean instanceof EmployeeRepository
                || bean instanceof EmployeeFormatter || bean instanceof FormatterHolder
                || bean instanceof EmployeeLifecycleProbe;
    }

    private boolean target(Object bean) {
        return !(bean instanceof BeanPostProcessor) && (!employeeOnly || employeeRelated(bean));
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        observe("BEFORE", bean, beanName);
        return bean; // ログ観測だけなので入力された参照をそのまま返す。nullや別オブジェクトに置き換えない。
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        observe("AFTER", bean, beanName);
        return bean;
    }

    private void observe(String phase, Object bean, String beanName) {
        if (!target(bean)) return;
        events.add(new Event(phase, beanName, bean));
        System.out.printf("[BPP %s] bean=%s type=%s%n", phase, beanName, bean.getClass().getSimpleName());
        if (bean instanceof EmployeeLifecycleProbe probe) {
            if (phase.equals("BEFORE")) lifecycle("02 BPP_BEFORE initialized=" + probe.isInitialized());
            else lifecycle("05 BPP_AFTER initialized=" + probe.isInitialized());
        }
    }

    void lifecycle(String marker) {
        lifecycle.add(marker);
        System.out.println("[LIFE " + marker + "]");
    }

    public List<Event> events() { return List.copyOf(events); }
    public List<String> lifecycleEvents() { return List.copyOf(lifecycle); }
    public boolean isEmployeeOnly() { return employeeOnly; }
}
