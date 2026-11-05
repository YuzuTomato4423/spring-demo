package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SPR-W6-D5-02: Repositoryの保存・関連検索とServiceの業務ルールをまとめて回帰確認する23件の一部。
 * SPR-W6-D4-03: 既存部署・未登録メールなら登録でき、メールは空白除去・小文字化して保存する。
 * 本人のメールを維持する更新は許可し、登録通知はTransactionのcommitが確定してから一度送る。
 */
@org.junit.jupiter.api.Tag("spr-w6-d5-repository-service")
@DisplayName("SPR-W6-D4-03 Service正常系")
class Week6Day4ServiceNormalTests extends Week6Day4ServiceSupport {
    @Test @DisplayName("SPR-W6-D4-03 重複なし登録・メール正規化・commit後のDBと通知")
    void registersUniqueEmployeeAndNormalizesEmail() {
        int before = notifier.getDeliveryCount();
        var created = service.create(request("正常登録", "  Alice@Example.TEST  ", 1));
        assertThat(created.id()).isPositive();
        assertThat(created.name()).isEqualTo("正常登録");
        assertThat(created.email()).isEqualTo("alice@example.test");
        assertThat(created.departmentName()).isEqualTo("開発部");
        assertThat(rows()).hasSize(1);
        assertThat(rows().get(0)).containsEntry("ID", created.id()).containsEntry("NAME", "正常登録")
                .containsEntry("EMAIL", "alice@example.test").containsEntry("DEPARTMENT_ID", 1);
        assertThat(service.findById(created.id())).isEqualTo(created);
        assertThat(notifier.getDeliveryCount()).isEqualTo(before + 1);
    }

    @Test @DisplayName("SPR-W6-D4-03 本人のメールを維持して氏名と部署を更新")
    void updatesOwnEmailAndDepartmentWithoutChangingId() {
        var original = service.create(request("更新前", "own@example.test", 1));
        int before = notifier.getDeliveryCount();
        var updated = service.update(original.id(), request("更新後", " OWN@EXAMPLE.TEST ", 2));
        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.name()).isEqualTo("更新後");
        assertThat(updated.email()).isEqualTo("own@example.test");
        assertThat(updated.departmentName()).isEqualTo("営業部");
        assertThat(rows()).hasSize(1);
        assertThat(rows().get(0)).containsEntry("ID", original.id()).containsEntry("NAME", "更新後")
                .containsEntry("EMAIL", "own@example.test").containsEntry("DEPARTMENT_ID", 2);
        assertThat(notifier.getDeliveryCount()).isEqualTo(before);
    }

    @Test @DisplayName("SPR-W6-D4-03 外側Transactionのcommit前は通知せずcommit後に一度通知")
    void notifiesOnlyAfterOutermostTransactionCommits() {
        int before = notifier.getDeliveryCount();
        var created = new TransactionTemplate(transactions).execute(status -> {
            var employee = service.create(request("commit待ち", "commit@example.test", 1));
            assertThat(notifier.getDeliveryCount()).isEqualTo(before);
            return employee;
        });
        assertThat(created).isNotNull();
        assertThat(rows()).hasSize(1);
        assertThat(rows().get(0)).containsEntry("ID", created.id());
        assertThat(notifier.getDeliveryCount()).isEqualTo(before + 1);
        assertThat(notifier.getLastResult()).contains("registered employee id=" + created.id());
    }
}
