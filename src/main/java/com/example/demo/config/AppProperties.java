package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 環境差分を外部設定へ集約し、表示と社員登録通知で同じ環境ラベルを使う。 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String message;
    // 実行環境を識別するラベル。Profileの有効化そのものはspring.profiles.activeが行う。
    private String environmentName;
    // SPR-1008-04: 環境変数から受け取る学習用サンプル。ログ/toStringには値を出さない。
    private String samplePassword;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getEnvironmentName() { return environmentName; }
    public void setEnvironmentName(String environmentName) { this.environmentName = environmentName; }
    public String getSamplePassword() { return samplePassword; }
    public void setSamplePassword(String samplePassword) { this.samplePassword = samplePassword; }
    public boolean isSamplePasswordConfigured() {
        return samplePassword != null && !samplePassword.isBlank();
    }
    @Override public String toString() {
        return "AppProperties[samplePassword=<redacted>]";
    }
}
