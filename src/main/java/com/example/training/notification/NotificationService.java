package com.example.training.notification;

/** 通知手段に依存しない共通契約。実習では外部送信せず結果を返す。 */
public interface NotificationService {
    String send(String message);
}
