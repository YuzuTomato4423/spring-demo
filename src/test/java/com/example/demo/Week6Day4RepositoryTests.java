package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.demo.entity.EmployeeEntity;

/**
 * SPR-W6-D5-02: Repositoryの保存・関連検索とServiceの業務ルールをまとめて回帰確認する23件の一部。
 * SPR-W6-D4-01: Repository単体でDBへの保存・再読取を確認し、
 * ControllerやServiceの成否と切り離してEntityの対応付けの誤りを検出する。
 * flushとclearで一度SQLへ書き出し、同一インスタンスの再利用による見かけの成功を防ぐ。
 */
@org.junit.jupiter.api.Tag("spr-w6-d5-repository-service")
@DisplayName("SPR-W6-D4-01 Repository save/find・CRUD")
class Week6Day4RepositoryTests extends Week6Day4JpaSupport {
    @Test @DisplayName("SPR-W6-D4-01 保存後にID・氏名・メール・部署をDBから再取得")
    void savesAndReloadsGeneratedIdAndAllFields() {
        var saved = employees.save(new EmployeeEntity("保存確認", "save@example.test", development));
        Long id = saved.getId();
        assertThat(id).isPositive();
        flushAndClear();

        var reloaded = employees.findById(id).orElseThrow();
        assertThat(reloaded).isNotSameAs(saved);
        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getName()).isEqualTo("保存確認");
        assertThat(reloaded.getEmail()).isEqualTo("save@example.test");
        assertThat(reloaded.getDepartmentId()).isEqualTo(1);
        assertThat(reloaded.getDepartment().getName()).isEqualTo("開発部");
    }

    @Test @DisplayName("SPR-W6-D4-01 未存在IDのfindByIdはempty")
    void missingIdReturnsEmpty() {
        assertThat(employees.findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test @DisplayName("SPR-W6-D4-01 更新後もIDを維持し変更値を再取得")
    void updatesSameRecordAndReloadsChangedFields() {
        Long id = employees.save(new EmployeeEntity("更新前", "before@example.test", development)).getId();
        flushAndClear();
        var employee = employees.findById(id).orElseThrow();
        employee.update("更新後", "after@example.test", departments.findById(2).orElseThrow());
        employees.save(employee);
        flushAndClear();

        var reloaded = employees.findById(id).orElseThrow();
        assertThat(reloaded.getId()).isEqualTo(id);
        assertThat(reloaded.getName()).isEqualTo("更新後");
        assertThat(reloaded.getEmail()).isEqualTo("after@example.test");
        assertThat(reloaded.getDepartmentId()).isEqualTo(2);
        assertThat(employees.count()).isEqualTo(1);
    }

    @Test @DisplayName("SPR-W6-D4-01 社員削除後はempty・共有部署は維持")
    void deletesEmployeeWithoutDeletingDepartment() {
        Long id = employees.save(new EmployeeEntity("削除確認", "delete@example.test", development)).getId();
        flushAndClear();
        employees.deleteById(id);
        flushAndClear();
        assertThat(employees.findById(id)).isEmpty();
        assertThat(employees.count()).isZero();
        assertThat(departments.findById(1)).isPresent();
        assertThat(departments.count()).isEqualTo(2);
    }
}
