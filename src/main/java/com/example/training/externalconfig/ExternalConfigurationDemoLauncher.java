package com.example.training.externalconfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import com.example.demo.SpringDemoApplication;
import com.example.demo.config.AppProperties;

/** SPR-1008-01～04。実際のBoot設定読込とHTTP応答を照合するEclipse用入口。 */
public final class ExternalConfigurationDemoLauncher {
    private ExternalConfigurationDemoLauncher() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !List.of("01", "02", "03", "04").contains(args[0])) {
            throw new IllegalArgumentException("課題番号01～04と、必要ならBootの--設定を指定してください。");
        }
        String stage = args[0];
        String[] bootArgs = Arrays.copyOfRange(args, 1, args.length);
        try (var context = new SpringApplicationBuilder(SpringDemoApplication.class)
                .bannerMode(Banner.Mode.OFF).logStartupInfo(false)
                .properties("server.address=127.0.0.1", "server.port=0", "logging.level.root=WARN", "spring.output.ansi.enabled=never")
                .run(bootArgs)) {
            var settings = context.getBean(AppProperties.class);
            String[] profiles = context.getEnvironment().getActiveProfiles();
            if (stage.equals("03") && (profiles.length != 1 || !List.of("dev", "prod").contains(profiles[0]))) {
                throw new IllegalArgumentException("課題03は--spring.profiles.active=dev または prodを指定してください。");
            }
            int port = ((WebServerApplicationContext) context).getWebServer().getPort();
            URI uri = URI.create("http://127.0.0.1:" + port + "/hello");
            var response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                    .send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || !response.body().equals(settings.getMessage())) {
                throw new IllegalStateException("設定Beanと実HTTPレスポンスが一致しません。");
            }
            System.out.println("=== G-1008-SPR / SPR-1008-" + stage + " : actual Spring Boot execution ===");
            System.out.println("Active profile: " + (profiles.length == 0 ? "default (none specified)" : String.join(",", profiles)));
            System.out.println("Configuration Bean: " + settings.getClass().getName());
            System.out.println("app.environment-name = " + settings.getEnvironmentName());
            System.out.println("app.message = " + settings.getMessage());
            System.out.println("HTTP GET " + uri + " -> " + response.statusCode());
            System.out.println("Response body = " + response.body());
            // SPR-1008-04: 設定有無だけを観測。秘密情報の値はログ/証跡へ出力しない。
            System.out.println("sample password configured = " + settings.isSamplePasswordConfigured() + " (value not logged)");
            System.out.println("Configuration -> AppProperties -> HelloController -> HTTP response: MATCH");
        }
        // close後にのみPASSを表示。過去のログや模擬出力を証跡に使わない。
        System.out.println("Context closed normally");
        System.out.println("=== SPR-1008-" + stage + " : PASS ===");
    }
}
