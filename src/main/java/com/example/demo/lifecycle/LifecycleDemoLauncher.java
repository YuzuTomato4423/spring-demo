package com.example.demo.lifecycle;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.example.demo.SpringDemoApplication;

/** Eclipseから実行する学習専用入口。既存の通常起動入口・設定は変更しない。 */
public class LifecycleDemoLauncher {
    public static void main(String[] args) {
        System.out.println("=== G-1002-SPR / SPR-1002-01..04 : actual execution ===");
        ConfigurableApplicationContext context = new SpringApplicationBuilder(SpringDemoApplication.class)
                .profiles("lifecycle-demo")
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false)
                .properties("logging.level.root=WARN", "spring.output.ansi.enabled=never")
                .run(args);
        LifecycleBean bean;
        LifecycleResource resource;
        try (context) {
            bean = context.getBean(LifecycleBean.class);
            resource = context.getBean(LifecycleResource.class);
            // 図のContext終了[LIFE 06]。強制終了に頼らずcloseで破棄callbackを実行する。
            System.out.println("[LIFE 06] CONTEXT_CLOSE_BEGIN active=" + context.isActive());
        }
        // try-with-resourcesのclose後に、実際の解放状態を確認する。callbackを直接呼ばない。
        System.out.println("[LIFE 09] CONTEXT_CLOSED active=" + context.isActive()
                + " resourceClosed=" + resource.isClosed() + " cacheSize=" + bean.getCacheSize());
        if (context.isActive() || !resource.isClosed() || bean.isInitialized() || bean.getCacheSize() != 0) {
            throw new IllegalStateException("Context終了後の破棄確認に失敗しました。");
        }
        System.out.println("=== SPR-1002-01 / 02 / 03 / 04 : PASS ===");
    }
}
