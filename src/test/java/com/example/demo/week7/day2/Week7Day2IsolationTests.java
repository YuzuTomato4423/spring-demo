package com.example.demo.week7.day2;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/**
 * SPR-W7-D2-03: 同じ一意メールを2回使い、各回の開始0件・commit後1件・後処理0件を確認する。
 * HTTPは別スレッドでcommitするためrollback任せにせず、共通のBeforeEach/AfterEachで初期化する。
 */
@DisplayName("SPR-W7-D2-03 DB Isolation")
class Week7Day2IsolationTests extends Week7Day2IntegrationSupport {
    @RepeatedTest(value = 2, name = "SPR-W7-D2-03 isolation {currentRepetition}/{totalRepetitions}")
    void repeatedCreateStartsFromCleanDatabase(RepetitionInfo repetition) throws Exception {
        assertThat(employeeCount()).isZero();
        var created = tree(send("POST", "/employees",
                employee("IsolationEmployee", "repeat-day2@example.test", 1), 201));
        long id = created.get("id").asLong();
        assertCommittedEmployee(id, "IsolationEmployee", "repeat-day2@example.test", 1);
        assertThat(tree(send("GET", "/employees/" + id, null, 200))).isEqualTo(created);
        System.out.printf("[SPR-W7-D2-03] PASS repetition %d/2 | before=0 committed=1 | same email reused%n",
                repetition.getCurrentRepetition());
    }
}
