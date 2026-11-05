package com.example.demo.notification;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.example.demo.config.AppProperties;
import com.example.demo.model.Employee;
import com.example.training.notification.NotificationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/** 共有singletonとして通知の準備状態を管理し、利用側から環境差分を隠す。 */
@Component
public class EmployeeRegistrationNotifier {
    private static final Logger log = LoggerFactory.getLogger(EmployeeRegistrationNotifier.class);
    private final NotificationService channel;
    private final AppProperties properties;
    private final AtomicInteger deliveryCount = new AtomicInteger();
    private volatile boolean ready;
    private String environmentName;
    private volatile String lastResult;

    /** Slack候補が増えても、社員登録通知の宛先を既定値に依存させない。 */
    public EmployeeRegistrationNotifier(
            @Qualifier("employeeRegistrationNotification") NotificationService channel,
            AppProperties properties) {
        this.channel = channel;
        this.properties = properties;
    }

    /** DI後に外部設定を確定し、準備が済んだ通知だけを業務処理へ公開する。 */
    @PostConstruct
    public void initialize() {
        environmentName = properties.getEnvironmentName();
        ready = true;
        log.info("[EMP-NOTIFY] INIT environment={} ready={}", environmentName, ready);
    }

    /** 保存済み社員の識別子だけを渡し、模擬通知でも不要な業務データを広げない。 */
    public String notifyRegistration(Employee employee) {
        return notifyRegistration((long) employee.getId());
    }

    /** SPR-W5-D2-03: JPAのLong IDを切り詰めず、保存確定後の模擬通知へ渡す。 */
    public String notifyRegistration(long employeeId) {
        if (!ready) {
            throw new IllegalStateException("通知Beanの準備が未完了、または終了済みです。");
        }
        String result = channel.send("[" + environmentName + "] registered employee id=" + employeeId);
        lastResult = result;
        deliveryCount.incrementAndGet();
        log.info("[EMP-NOTIFY] {}", result);
        return result;
    }

    /** Context終了後に保持された参照から通知を再開できないようにする。 */
    @PreDestroy
    public void release() {
        ready = false;
        log.info("[EMP-NOTIFY] DESTROY environment={} ready={}", environmentName, ready);
    }

    public boolean isReady() { return ready; }
    public int getDeliveryCount() { return deliveryCount.get(); }
    public String getLastResult() { return lastResult; }
    public String getEnvironmentName() { return environmentName; }
}
