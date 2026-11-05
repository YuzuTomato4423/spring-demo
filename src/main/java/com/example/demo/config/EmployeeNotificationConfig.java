package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.example.training.notification.NotificationService;
import com.example.training.notification.SlackNotificationService;
import com.example.training.profilelazy.ProfileLazyConfigurations.DevNotificationService;
import com.example.training.profilelazy.ProfileLazyConfigurations.ProdNotificationService;

/** 社員登録の通知方式だけを環境で差し替え、業務処理から環境分岐を分離する。 */
@Configuration(proxyBeanMethods = false)
public class EmployeeNotificationConfig {
    @Bean("employeeRegistrationNotification")
    @Profile("!prod")
    public NotificationService developmentRegistrationNotification() {
        return new DevNotificationService();
    }

    @Bean("employeeRegistrationNotification")
    @Profile("prod")
    public NotificationService productionRegistrationNotification() {
        return new ProdNotificationService();
    }

    /** チーム共有用の別候補。社員登録の個別通知にはQualifierで別Beanを選ぶ。 */
    @Bean
    public NotificationService slackNotificationService() {
        return new SlackNotificationService();
    }
}
