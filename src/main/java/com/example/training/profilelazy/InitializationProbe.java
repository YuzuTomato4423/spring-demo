package com.example.training.profilelazy;

import java.util.ArrayList;
import java.util.List;

/** 重いBeanを取得せずに生成回数と初期化順序を観察する、Contextごとの記録。 */
public final class InitializationProbe {
    private final long start = System.nanoTime();
    private final List<String> events = new ArrayList<>();
    private int created;
    private int initialized;

    public void record(String event) {
        events.add(event);
        System.out.printf("[REPORT] +%dms %s created=%d initialized=%d%n",
                (System.nanoTime() - start) / 1_000_000, event, created, initialized);
    }

    public void constructed() { created++; record("CONSTRUCTOR"); }
    public void initialized() { initialized++; record("INIT_DONE"); }
    public int createdCount() { return created; }
    public int initializedCount() { return initialized; }
    public List<String> events() { return List.copyOf(events); }
}
