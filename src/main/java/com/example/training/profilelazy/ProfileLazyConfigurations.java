package com.example.training.profilelazy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import com.example.training.notification.NotificationService;

/** 通常アプリの走査範囲外。専用入口が必要な設定だけを登録する。 */
public final class ProfileLazyConfigurations {
    private ProfileLazyConfigurations() { }

    public static final class DevNotificationService implements NotificationService {
        @Override public String send(String message) { return "DEV_CONSOLE (simulation): " + message; }
    }

    public static final class ProdNotificationService implements NotificationService {
        @Override public String send(String message) { return "PROD_MAIL (simulation): " + message; }
    }

    public record NotificationClient(NotificationService service) { }

    @Configuration(proxyBeanMethods = false)
    public static class Notifications {
        // SPR-1006-01: devは開発時の確認用。実際の宛先へ配信せずコンソール結果を返す。
        @Bean @Profile("dev")
        public DevNotificationService devNotificationService() { return new DevNotificationService(); }

        // SPR-1006-01/02: prodは本番のメール通知を想定する別実装。学習では送信を模擬する。
        // Profileは起動設定で選び、業務コードを環境ごとに書き換えない。
        @Bean @Profile("prod")
        public ProdNotificationService prodNotificationService() { return new ProdNotificationService(); }

        @Bean
        public NotificationClient notificationClient(NotificationService service) {
            return new NotificationClient(service);
        }
    }

    @Configuration(proxyBeanMethods = false)
    public static class LazyReport {
        // SPR-1006-03: 任意の社員レポートの重い初期化を初回要求まで遅らせる。
        // eager singletonへ直接注入すると起動時に生成されるため、利用側は初回にgetBeanする。
        @Bean @Lazy
        public HeavyEmployeeReport heavyEmployeeReport(InitializationProbe probe) {
            return new HeavyEmployeeReport(probe);
        }
    }

    @Configuration(proxyBeanMethods = false)
    public static class EagerReport {
        // SPR-1006-04: 同じBeanと同じ処理を@Lazyなしで登録し、初期化の時点だけを比較する。
        @Bean
        public HeavyEmployeeReport heavyEmployeeReport(InitializationProbe probe) {
            return new HeavyEmployeeReport(probe);
        }
    }
}
