package com.example.training.notification;

/** チームで共有する即時通知を想定したSlack実装。実習では外部APIを呼ばない。 */
public class SlackNotificationService implements NotificationService {
    @Override
    public String send(String message) {
        return "SLACK: " + message;
    }
}
