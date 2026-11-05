package com.example.demo.training.relations;

import java.util.List;

/**
 * SPR-W6-D2-04: Entityの双方向関連をJSONへ直接公開すると部署→社員→部署と循環する。
 * 公開DTOを一方向に限定し、社員側にdepartmentを持たせず応答項目を制御する。
 */
public record DepartmentResponse(Integer id, String name, List<EmployeeItem> employees) {
    public record EmployeeItem(Long id, String name) { }
}
