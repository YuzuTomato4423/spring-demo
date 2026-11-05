package com.example.demo.lifecycle;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

import com.example.demo.SpringDemoApplication;

@ExtendWith(OutputCaptureExtension.class)
class LifecycleDemoTests {
    @Test
    void callbacksFollowDiInitializationUseAndCloseOrderExactlyOnce(CapturedOutput output) {
        ConfigurableApplicationContext context = application().profiles("lifecycle-demo").run();
        LifecycleBean bean;
        LifecycleResource resource;
        try (context) {
            bean = context.getBean(LifecycleBean.class);
            resource = context.getBean(LifecycleResource.class);
            assertSame(resource, bean.getResource());
            assertTrue(bean.isInitialized());
            assertTrue(resource.isInitialized());
            assertEquals(1, bean.getCacheSize());
            assertTrue(output.getOut().contains("[DI CHECK] sameAsContext=true"));
            assertTrue(output.getOut().contains("[LIFE 05] USE initialized=true message=Dependency is ready"));
        }
        context.close(); // 二重closeでもcallbackを重複実行しないことを検証する。
        assertFalse(context.isActive());
        assertFalse(bean.isInitialized());
        assertEquals(0, bean.getCacheSize());
        assertTrue(resource.isClosed());
        assertFalse(resource.isInitialized());
        assertThrows(IllegalStateException.class, bean::use);
        assertThrows(IllegalStateException.class, resource::readMessage);
        int previous = -1;
        for (String marker : new String[] { "[LIFE 01] RESOURCE_CONSTRUCTOR", "[LIFE 02] RESOURCE_POST_CONSTRUCT",
                "[LIFE 03] CONSTRUCTOR_DI", "[LIFE 04] POST_CONSTRUCT", "[LIFE 05] USE",
                "[LIFE 07] PRE_DESTROY", "[LIFE 08] RESOURCE_PRE_DESTROY" }) {
            int position = output.getOut().indexOf(marker);
            assertTrue(position > previous, "実行順序が不正: " + marker);
            assertEquals(position, output.getOut().lastIndexOf(marker), "callbackが重複: " + marker);
            previous = position;
        }
    }

    @Test
    void normalApplicationDoesNotActivateLifecycleDemo() {
        try (ConfigurableApplicationContext context = application().run()) {
            assertTrue(context.getBeansOfType(LifecycleBean.class).isEmpty());
            assertTrue(context.getBeansOfType(LifecycleResource.class).isEmpty());
            assertTrue(context.getBeansOfType(LifecycleDemoRunner.class).isEmpty());
        }
    }

    @Test
    void eclipseEntryPointClosesContextAndReportsSuccessfulCleanup(CapturedOutput output) {
        LifecycleDemoLauncher.main(new String[0]);
        int use = output.getOut().indexOf("[LIFE 05]");
        int close = output.getOut().indexOf("[LIFE 06]");
        int destroy = output.getOut().indexOf("[LIFE 07]");
        int closed = output.getOut().indexOf("[LIFE 09]");
        assertTrue(use >= 0 && use < close && close < destroy && destroy < closed);
        assertTrue(output.getOut().contains("CONTEXT_CLOSED active=false resourceClosed=true cacheSize=0"));
        assertTrue(output.getOut().contains("=== SPR-1002-01 / 02 / 03 / 04 : PASS ==="));
    }

    private SpringApplicationBuilder application() {
        return new SpringApplicationBuilder(SpringDemoApplication.class)
                .web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                .properties("logging.level.root=WARN", "spring.output.ansi.enabled=never");
    }
}
