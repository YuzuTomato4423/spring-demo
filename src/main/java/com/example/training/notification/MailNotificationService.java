package com.example.training.notification;

/** 個別の宛先への案内や記録に残す通知を想定したMail実装。実習では送信を模擬する。 */
public class MailNotificationService implements NotificationService {
    @Override
    public String send(String message) {
        return "MAIL: " + message;
    }
}
