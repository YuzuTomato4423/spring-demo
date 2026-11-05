package com.example.demo.lifecycle;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/** SPR-1002-01～04: 生成/DI→初期化→利用→破棄を観察するsingleton Bean。 */
@Component
@Profile("lifecycle-demo")
public class LifecycleBean {
    private final LifecycleResource resource;
    private final List<String> cache = new ArrayList<>();
    private boolean initialized;

    public LifecycleBean(LifecycleResource resource) {
        // 図の「生成 + DI」[LIFE 03]。単一constructorにSpringが依存Beanを渡す。
        // constructor injectionでは生成とDIが一体。ここでは依存先を保持し、利用準備は分離する。
        this.resource = resource;
        System.out.println("[LIFE 03] CONSTRUCTOR_DI bean=" + identity()
                + " dependency=" + resource.identity() + " dependencyReady=" + resource.isInitialized());
    }

    @PostConstruct
    public void initialize() {
        // 図の「初期化」[LIFE 04]。注入済みの依存先でキャッシュを準備する。
        // constructor/DIの後なので依存先を利用でき、初期化完了後に業務メソッドを呼べる。
        if (!resource.isInitialized()) {
            throw new IllegalStateException("依存Beanの初期化が完了していません。");
        }
        cache.add(resource.readMessage());
        initialized = true;
        System.out.println("[LIFE 04] POST_CONSTRUCT dependencyReady=" + resource.isInitialized()
                + " cacheSize=" + cache.size() + " initialized=" + initialized);
    }

    public String use() {
        // 図の「利用」[LIFE 05]。初期化済みBeanだけをRunnerから利用する。
        if (!initialized) {
            throw new IllegalStateException("Beanの初期化が未完了、または破棄済みです。");
        }
        String message = cache.get(0);
        System.out.println("[LIFE 05] USE initialized=" + initialized + " message=" + message);
        return message;
    }

    @PreDestroy
    public void release() {
        // 図の「破棄」[LIFE 07]。Context.close()によりSpringが呼び出すcallback。
        // キャッシュの参照を解放し、破棄後の誤利用を防ぐ。依存BeanのReaderは依存Bean自身が閉じる。
        cache.clear();
        initialized = false;
        System.out.println("[LIFE 07] PRE_DESTROY cacheSize=" + cache.size() + " initialized=" + initialized);
    }

    public LifecycleResource getResource() { return resource; }
    public boolean isInitialized() { return initialized; }
    public int getCacheSize() { return cache.size(); }

    public String identity() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }
}
