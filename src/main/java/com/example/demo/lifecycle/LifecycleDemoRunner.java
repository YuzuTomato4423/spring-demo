package com.example.demo.lifecycle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 初期化済みBeanを利用し、SPR-1002-03/04のDI照合と順序図を表示する。 */
@Component
@Profile("lifecycle-demo")
public class LifecycleDemoRunner implements CommandLineRunner {
    private final LifecycleBean bean;
    private final ApplicationContext context;

    public LifecycleDemoRunner(LifecycleBean bean, ApplicationContext context) {
        this.bean = bean;
        this.context = context;
    }

    @Override
    public void run(String... args) throws IOException {
        // 手動newではなく、Springが生成した依存Beanと注入参照が同じことを確認する。
        boolean sameDependency = bean.getResource() == context.getBean(LifecycleResource.class);
        if (!sameDependency || !bean.isInitialized()) {
            throw new IllegalStateException("DIまたはBean初期化の検証に失敗しました。");
        }
        System.out.println("[DI CHECK] sameAsContext=" + sameDependency
                + " dependency=" + bean.getResource().identity());
        bean.use();
        // ソースのコメント・図・実行ログは共通の[LIFE nn]で対応付ける。
        try (var diagram = getClass().getResourceAsStream("/lifecycle-demo-diagram.txt")) {
            if (diagram == null) throw new IllegalStateException("Lifecycle順序図がありません。");
            System.out.print(new String(diagram.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
