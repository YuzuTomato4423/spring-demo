package com.example.training.notification;

import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.annotation.Resource;

/** Springが注入した参照を使う利用側。実装の手動生成による選択は行わない。 */
public final class NotificationClients {
    private NotificationClients() { }

    public static class TypeOnly {
        private final NotificationService service;

        // 引数名をBean名と一致させず、型だけの競合とPrimaryによる解決を比較する。
        public TypeOnly(NotificationService notificationService) {
            this.service = notificationService;
        }

        public NotificationService service() { return service; }
    }

    public static class MailQualified {
        private final NotificationService service;

        // 同じ型のBeanが複数あるため、Mailを必要とする利用側の意図を明示する。
        // Qualifierが型候補をMailへ絞るので、SlackにPrimaryがあってもMailを注入する。
        public MailQualified(@Qualifier("mailNotificationService") NotificationService notificationService) {
            this.service = notificationService;
        }

        public NotificationService service() { return service; }
    }

    public static class NamedMail {
        private NotificationService service;

        // Resourceは特定のBean名を参照する。コンストラクタ引数には使えないためsetterを使う。
        @Resource(name = "mailNotificationService")
        public void setService(NotificationService notificationService) {
            this.service = notificationService;
        }

        public NotificationService service() { return service; }
    }
}
