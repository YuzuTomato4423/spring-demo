package com.example.training.beanpostprocessor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.InitializingBean;
import com.example.demo.service.EmployeeService;

/** 図の01～09に対応する実測用Bean。callbackはSpringに任せ、Launcherから直接呼び出さない。 */
public final class EmployeeLifecycleProbe implements InitializingBean {
    private final EmployeeService service;
    private final EmployeeBeanPostProcessor observer;
    private boolean initialized;
    private boolean destroyed;

    public EmployeeLifecycleProbe(EmployeeService service, EmployeeBeanPostProcessor observer) {
        this.service = service;
        this.observer = observer;
        observer.lifecycle("01 CONSTRUCTOR_DI serviceInjected=" + (service != null));
    }

    @PostConstruct
    void initialize() {
        initialized = true;
        // 図03: CommonAnnotationBeanPostProcessorのbefore処理中に呼ばれる。
        observer.lifecycle("03 POST_CONSTRUCT initialized=" + initialized);
    }

    @Override
    public void afterPropertiesSet() {
        // 図04: beforeチェーン完了後のSpring初期化callback。図03との違いも実ログで示す。
        if (!initialized) throw new IllegalStateException("@PostConstructが先に実行されていません。");
        observer.lifecycle("04 AFTER_PROPERTIES_SET initialized=" + initialized);
    }

    public void use() {
        if (!initialized || destroyed) throw new IllegalStateException("利用可能なLifecycle状態ではありません。");
        observer.lifecycle("06 USE employeeCount=" + service.findAll().size());
    }

    @PreDestroy
    void destroy() {
        // 図08: Context.closeで破棄。初期化前後のBPP callbackとは別の段階。
        initialized = false;
        destroyed = true;
        observer.lifecycle("08 PRE_DESTROY initialized=false destroyed=true");
    }

    public boolean isInitialized() { return initialized; }
    public boolean isDestroyed() { return destroyed; }
    public EmployeeService service() { return service; }
}
