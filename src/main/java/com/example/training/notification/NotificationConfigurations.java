package com.example.training.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** 段階ごとに必要な設定だけを専用Contextへ登録し、比較を再現可能にする。 */
public final class NotificationConfigurations {
    private NotificationConfigurations() { }

    @Configuration(proxyBeanMethods = false)
    public static class TwoBeans {
        @Bean
        public MailNotificationService mailNotificationService() {
            return new MailNotificationService();
        }

        @Bean
        public SlackNotificationService slackNotificationService() {
            return new SlackNotificationService();
        }
    }

    @Configuration(proxyBeanMethods = false)
    public static class SlackPrimary {
        @Bean
        public MailNotificationService mailNotificationService() {
            return new MailNotificationService();
        }

        // チーム共有を既定の通知手段とし、特別な指定のない単一値の注入でSlackを選ぶ。
        // Mailが必要な利用側はQualifierで明示するため、既定値の変更に影響されない。
        @Bean
        @Primary
        public SlackNotificationService slackNotificationService() {
            return new SlackNotificationService();
        }
    }
}
