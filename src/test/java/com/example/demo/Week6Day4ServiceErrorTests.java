package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.demo.exception.DepartmentNotFoundException;
import com.example.demo.exception.DuplicateEmployeeEmailException;

/**
 * SPR-W6-D5-02: Repositoryの保存・関連検索とServiceの業務ルールをまとめて回帰確認する23件の一部。
 * SPR-W6-D4-04: 重複メール・部署未存在の期待例外を自動化し、業務ルールの退行を検出する。
 * 例外の発生だけで成功にせず、DBが更新されないことと失敗時の通知抑止も確認する。
 * 登録と更新の両入口を検証し、更新途中で既存社員を書き換える不具合も防ぐ。
 */
@org.junit.jupiter.api.Tag("spr-w6-d5-repository-service")
@DisplayName("SPR-W6-D4-04 Service異常系・期待例外")
class Week6Day4ServiceErrorTests extends Week6Day4ServiceSupport {
    @Test @DisplayName("SPR-W6-D4-04 重複登録はDuplicateEmployeeEmailException・DBと通知不変")
    void rejectsDuplicateCreateWithoutWritingOrNotifying() {
        service.create(request("登録済み", "duplicate@example.test", 1));
        var before = rows();
        int notifications = notifier.getDeliveryCount();
        assertThatExceptionOfType(DuplicateEmployeeEmailException.class).isThrownBy(() ->
                service.create(request("重複登録", " DUPLICATE@EXAMPLE.TEST ", 2)));
        assertThat(rows()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    @Test @DisplayName("SPR-W6-D4-04 他人のメールへ更新はDuplicateEmployeeEmailException・不変")
    void rejectsDuplicateUpdateWithoutChangingOriginalRecord() {
        service.create(request("社員A", "a@example.test", 1));
        var employeeB = service.create(request("社員B", "b@example.test", 1));
        var before = rows();
        int notifications = notifier.getDeliveryCount();
        assertThatExceptionOfType(DuplicateEmployeeEmailException.class).isThrownBy(() ->
                service.update(employeeB.id(), request("変更されない", " A@EXAMPLE.TEST ", 2)));
        assertThat(rows()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    @Test @DisplayName("SPR-W6-D4-04 未存在部署への登録はDepartmentNotFoundException・不変")
    void rejectsMissingDepartmentOnCreate() {
        service.create(request("既存社員", "existing@example.test", 1));
        var before = rows();
        int notifications = notifier.getDeliveryCount();
        assertThatExceptionOfType(DepartmentNotFoundException.class).isThrownBy(() ->
                service.create(request("登録されない", "missing@example.test", 999)));
        assertThat(rows()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }

    @Test @DisplayName("SPR-W6-D4-04 未存在部署への更新はDepartmentNotFoundException・不変")
    void rejectsMissingDepartmentOnUpdate() {
        var employee = service.create(request("既存社員", "keep@example.test", 1));
        var before = rows();
        int notifications = notifier.getDeliveryCount();
        assertThatExceptionOfType(DepartmentNotFoundException.class).isThrownBy(() ->
                service.update(employee.id(), request("変更されない", "changed@example.test", 999)));
        assertThat(rows()).isEqualTo(before);
        assertThat(notifier.getDeliveryCount()).isEqualTo(notifications);
    }
}
