package com.example.training.externalconfig;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import com.example.demo.HelloController;
import com.example.demo.SpringDemoApplication;
import com.example.demo.config.AppProperties;

@ExtendWith(OutputCaptureExtension.class)
class ExternalConfigurationTests {
    private SpringApplicationBuilder builder() {
        return new SpringApplicationBuilder(SpringDemoApplication.class).bannerMode(Banner.Mode.OFF)
                .logStartupInfo(false).properties("server.address=127.0.0.1", "logging.level.root=WARN", "server.port=0");
    }

    @Test void defaultAndProfilesChangeRealHttpResponse() throws Exception {
        for (String profile : new String[]{"", "dev", "prod"}) {
            String[] args = profile.isEmpty() ? new String[0] : new String[]{"--spring.profiles.active=" + profile};
            try (var context = builder().run(args)) {
                var properties = context.getBean(AppProperties.class);
                String expected = "Hello Spring!" + (profile.isEmpty() ? "" : " [" + profile + "]");
                assertEquals(expected, properties.getMessage());
                assertEquals(profile.isEmpty() ? "default" : profile, properties.getEnvironmentName());
                assertEquals(expected, context.getBean(HelloController.class).hello());
                int port = ((WebServerApplicationContext)context).getWebServer().getPort();
                var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:"+port+"/hello")).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertEquals(expected, response.body());
            }
        }
    }

    @Test void runtimeMessageOverridesProfileWithoutCodeChange() {
        try (var context = builder().web(WebApplicationType.NONE)
                .run("--spring.profiles.active=dev", "--app.message=Runtime override")) {
            assertEquals("Runtime override", context.getBean(HelloController.class).hello());
            assertEquals("dev", context.getBean(AppProperties.class).getEnvironmentName());
        }
    }

    @Test void environmentVariableBindsSampleAndDoesNotLeak(CapturedOutput output) {
        String sample = "FAKE-SPR1008-TEST-ONLY";
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("sample-test-env",
                Map.of("SPR1008_SAMPLE_PASSWORD", sample)));
        try (var context = builder().environment(environment).web(WebApplicationType.NONE).run()) {
            var settings = context.getBean(AppProperties.class);
            assertEquals(sample, settings.getSamplePassword());
            assertTrue(settings.isSamplePasswordConfigured());
            assertFalse(settings.toString().contains(sample));
        }
        assertFalse(output.getAll().contains(sample));
    }

    @Test void unsetSampleIsSafeAndBlank() {
        try (var context = builder().web(WebApplicationType.NONE).run("--app.sample-password=")) {
            assertFalse(context.getBean(AppProperties.class).isSamplePasswordConfigured());
        }
    }

    @Test void launcherLogsActualHttpAndMasksSample(CapturedOutput output) throws Exception {
        String sample = "FAKE-LAUNCHER-TEST-ONLY";
        ExternalConfigurationDemoLauncher.main(new String[]{"04", "--app.sample-password=" + sample});
        assertTrue(output.getOut().contains("SPR-1008-04 : PASS"));
        assertTrue(output.getOut().contains("Response body = Hello Spring!"));
        assertTrue(output.getOut().contains("sample password configured = true (value not logged)"));
        assertFalse(output.getAll().contains(sample));
    }

    @Test void invalidStageFailsBeforeStartingApplication() {
        assertThrows(IllegalArgumentException.class, () -> ExternalConfigurationDemoLauncher.main(new String[0]));
        assertThrows(IllegalArgumentException.class, () -> ExternalConfigurationDemoLauncher.main(new String[]{"05"}));
    }
}
