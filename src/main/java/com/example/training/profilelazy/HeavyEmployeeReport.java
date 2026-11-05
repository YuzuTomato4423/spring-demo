package com.example.training.profilelazy;

import java.util.List;
import jakarta.annotation.PostConstruct;
import com.example.demo.model.Employee;

/** 任意のレポート機能。約1秒の初期化コストを同一条件で模擬する。 */
public final class HeavyEmployeeReport {
    private final InitializationProbe probe;
    private boolean ready;

    public HeavyEmployeeReport(InitializationProbe probe) {
        this.probe = probe;
        probe.constructed();
    }

    @PostConstruct
    public void initialize() {
        probe.record("INIT_BEGIN simulatedCost=1000ms");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("レポートの初期化が中断されました。", error);
        }
        ready = true;
        probe.initialized();
    }

    public String generate(List<Employee> employees) {
        if (!ready) throw new IllegalStateException("レポートの初期化が完了していません。");
        return "Employee report: count=" + employees.size()
                + " ids=" + employees.stream().map(Employee::getId).toList();
    }
}
