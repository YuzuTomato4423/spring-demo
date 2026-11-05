package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import com.example.demo.entity.EmployeeEntity;

/**
 * SPR-W6-D5-02: Repositoryの保存・関連検索とServiceの業務ルールをまとめて回帰確認する23件の一部。
 * SPR-W6-D4-02: 田中太郎(開発)・田中花子(営業)で氏名だけの検索と部署ANDを区別する。
 * 佐藤花子(開発)も置き、部署だけの絞込みやORへの取り違えを検出する。
 * 通常版と実Serviceで使うSort版を同じデータで検証し、取得順はSort版だけで保証する。
 */
@org.junit.jupiter.api.Tag("spr-w6-d5-repository-service")
@DisplayName("SPR-W6-D4-02 氏名・部署Query Method")
class Week6Day4QueryTests extends Week6Day4JpaSupport {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "id");

    @BeforeEach void prepareDistinctSearchCandidates() {
        employees.save(new EmployeeEntity("田中太郎", "taro@example.test", development));
        employees.save(new EmployeeEntity("田中花子", "tanaka-hanako@example.test", sales));
        employees.save(new EmployeeEntity("佐藤花子", "sato@example.test", development));
        flushAndClear();
    }

    @ParameterizedTest(name = "氏名部分一致 Sort={0}") @ValueSource(booleans = {false, true})
    @DisplayName("SPR-W6-D4-02 氏名は部分一致し別部署の田中も含む")
    void nameMatchesPartOfName(boolean sorted) {
        var result = sorted ? employees.findByNameContaining("田中", NEWEST_FIRST)
                : employees.findByNameContaining("田中");
        expectNames(result, sorted, "田中花子", "田中太郎");
    }

    @ParameterizedTest(name = "部署完全一致 Sort={0}") @ValueSource(booleans = {false, true})
    @DisplayName("SPR-W6-D4-02 開発部の異なる氏名の社員を取得")
    void departmentMatchesExactDepartmentName(boolean sorted) {
        var result = sorted ? employees.findByDepartment_Name("開発部", NEWEST_FIRST)
                : employees.findByDepartment_Name("開発部");
        expectNames(result, sorted, "佐藤花子", "田中太郎");
    }

    @ParameterizedTest(name = "氏名AND部署 Sort={0}") @ValueSource(booleans = {false, true})
    @DisplayName("SPR-W6-D4-02 田中かつ開発部だけを取得")
    void combinedQueryUsesAnd(boolean sorted) {
        var result = sorted ? employees.findByNameContainingAndDepartment_Name("田中", "開発部", NEWEST_FIRST)
                : employees.findByNameContainingAndDepartment_Name("田中", "開発部");
        expectNames(result, sorted, "田中太郎");
    }

    @ParameterizedTest(name = "氏名該当なし Sort={0}") @ValueSource(booleans = {false, true})
    @DisplayName("SPR-W6-D4-02 未登録氏名は空リスト")
    void unmatchedNameReturnsEmpty(boolean sorted) {
        assertThat(sorted ? employees.findByNameContaining("山田", NEWEST_FIRST)
                : employees.findByNameContaining("山田")).isEmpty();
    }

    @ParameterizedTest(name = "部署名部分一致を拒否 Sort={0}") @ValueSource(booleans = {false, true})
    @DisplayName("SPR-W6-D4-02 部署名の一部分では一致しない")
    void partialDepartmentNameReturnsEmpty(boolean sorted) {
        assertThat(sorted ? employees.findByDepartment_Name("開発", NEWEST_FIRST)
                : employees.findByDepartment_Name("開発")).isEmpty();
    }

    @ParameterizedTest(name = "ANDの交差条件該当なし Sort={0}") @ValueSource(booleans = {false, true})
    @DisplayName("SPR-W6-D4-02 氏名と部署が別社員に一致しても空")
    void crossedConditionsDoNotBroadenSearch(boolean sorted) {
        assertThat(sorted ? employees.findByNameContainingAndDepartment_Name("佐藤", "営業部", NEWEST_FIRST)
                : employees.findByNameContainingAndDepartment_Name("佐藤", "営業部")).isEmpty();
    }

    private void expectNames(List<EmployeeEntity> result, boolean sorted, String... expected) {
        var names = result.stream().map(EmployeeEntity::getName).toList();
        if (sorted) assertThat(names).containsExactly(expected);
        else assertThat(names).containsExactlyInAnyOrder(expected);
    }
}
